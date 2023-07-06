/*
 * Copyright 2022 Valtech GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.valtech.aapm.restrictions;

import com.day.cq.dam.api.DamConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionPattern;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class SubFolderPattern implements RestrictionPattern {

    private static final Logger LOG = LoggerFactory.getLogger(SubFolderPattern.class);
    public static final String CONTENT_DAM = "/content/dam";
    private final String originalTree;
    private final Integer level;
    private final String permissionType;
    private static final String DENY = "deny";
    List<String> operators = List.of("_EQUALS_", "_GREATER_THAN_EQUALS_", "_LESS_THAN_EQUALS_", "_GREATER_THEN_", "_LESS_THEN_");
    private String operator = operators.get(0);
    private final boolean negate;


    SubFolderPattern(String propertyValues, String originalTree) {
        this.originalTree = originalTree; // The originalTree here is where the permission is set
        permissionType = propertyValues.split("_")[0];
        String propertyValuesWithoutPermissionType = StringUtils.removeStart(propertyValues, permissionType + "_");
        if (propertyValuesWithoutPermissionType.startsWith("!")) {
            negate = true;
        } else {
            negate = false;
        }
        for (int i = 0; i < operators.size(); i++) {
            if (propertyValuesWithoutPermissionType.contains(operators.get(i))) {
                operator = operators.get(i);
                break;
            }
        }
        String sLevel = propertyValuesWithoutPermissionType.split(operator)[1];
        level = Integer.valueOf(sLevel);
    }

    static RestrictionPattern create(PropertyState stringProperty, String originalTree) {
        if (stringProperty.count() == 1) {
            return new SubFolderPattern(stringProperty.getValue(Type.STRING), originalTree);
        } else {
            return RestrictionPattern.EMPTY;
        }
    }

    /**
     * Return the first node of type Folder or Asset.
     *
     * @param tree the tree to test
     * @return a @{@link Tree} of type Folder or Asset
     */
    private Tree getFirstParentOfTypeFolderOrAsset(Tree tree) {
        Tree assetTree = getDamAssetParent(tree);
        if (assetTree != null) {
            return assetTree;
        }
        Tree folderTree = getFolderParent(tree);
        if (folderTree != null) {
            return folderTree;
        }
        return tree;
    }

    /**
     * Return the tree corresponding to an asset if it exists.
     * It stops searching at the DAM root level (/content/dam).
     * Ex:
     * - /content/dam/test-folder/my-asset.jpg/renditions/thumbnail.jpg will return /content/dam/test-folder/my-asset.jpg
     * - /content/dam/test-folder/my-asset.jpg will return /content/dam/test-folder/my-asset.jpg
     * - /content/dam/test-folder will return null
     *
     * @param tree The original tree to parse recursively.
     * @return the first tree that match an asset of null if nothing has been found.
     */
    private Tree getDamAssetParent(Tree tree) {
        if (tree == null) {
            return null;
        }

        if (isAsset(tree)) {
            return tree;
        }

        Tree parent = tree.getParent();
        if (parent.getPath().equals(CONTENT_DAM)) {
            return null;
        }
        return getDamAssetParent(parent);
    }

    /**
     * Return the tree corresponding to a folder if it exists.
     * It stops searching at the DAM root level (/content/dam).
     * Ex:
     * - /content/dam/test-folder/my-asset.jpg/renditions/thumbnail.jpg will return /content/dam/test-folder/my-asset.jpg/renditions
     * - /content/dam/test-folder/my-asset.jpg will return /content/dam/test-folder
     * - /content/dam/test-folder will return /content/dam/test-folder
     *
     * @param tree The original tree to parse recursively.
     * @return the first tree that match a folder of null if nothing has been found.
     */
    private Tree getFolderParent(Tree tree) {
        if (tree == null) {
            return null;
        }

        if (isFolder(tree)) {
            return tree;
        }

        Tree parent = tree.getParent();
        if (parent.getPath().equals(CONTENT_DAM)) {
            return parent;
        }
        return getFolderParent(parent);
    }

    /**
     * Return true if the specified tree is an asset (jcr:primaryType = dam:Asset).
     *
     * @param tree the tree to check.
     * @return true if it's an asset, false otherwise.
     */
    private boolean isAsset(Tree tree) {
        PropertyState ps = tree.getProperty(JcrConstants.JCR_PRIMARYTYPE);
        if (ps != null) {
            String type = ps.getValue(Type.STRING);
            return DamConstants.NT_DAM_ASSET.equalsIgnoreCase(type);
        }
        return false;
    }

    /**
     * Return true if the specified tree is a folder.
     * The folder types can be:
     * - nt:Folder
     * - sling:Folder
     * - sling:OrderedFolder
     *
     * @param tree the tree to check.
     * @return true if it's a folder, false otherwise.
     */
    private boolean isFolder(Tree tree) {
        PropertyState ps = tree.getProperty(JcrConstants.JCR_PRIMARYTYPE);
        if (ps != null) {
            String type = ps.getValue(Type.STRING);
            return JcrResourceConstants.NT_SLING_ORDERED_FOLDER.equalsIgnoreCase(type)
                    || JcrConstants.NT_FOLDER.equalsIgnoreCase(type)
                    || JcrResourceConstants.NT_SLING_FOLDER.equalsIgnoreCase(type);
        }
        return false;
    }

    private boolean checkTree(Tree tree) {
        if (hasMetadataFolder(tree)) {
            return isMatch(tree); //TODO This is the basic one, investigate for the other descendant cases
        }
        return false;
    }

    private boolean hasMetadataFolder(Tree tree) {
        return tree.hasChild(JcrConstants.JCR_CONTENT)
                && tree.getChild(JcrConstants.JCR_CONTENT)
                .hasChild(DamConstants.ACTIVITY_TYPE_METADATA);
    }

    @Override
    public boolean matches(Tree tree, PropertyState propertyState) {
        if (isRuleToApplyADeny()) {
            return denyMatch(tree);
        }
        if (isRuleToApplyAnAllow()) {
            return isMatch(tree);
        }
        return false;
    }

    private boolean allowMatch(Tree tree) {
        Tree firstParentOfTypeFolderOrAsset = getFirstParentOfTypeFolderOrAsset(tree);
        if (isAsset(firstParentOfTypeFolderOrAsset)) // This is an asset
        {
            boolean ret = negate != checkTree(firstParentOfTypeFolderOrAsset);
            LOG.debug("allowMatch for tree of type Asset {} Match:: {}", firstParentOfTypeFolderOrAsset.getName(), ret);
            return ret;
        }

        if (isFolder(firstParentOfTypeFolderOrAsset)) {
            for (Tree currentTree : firstParentOfTypeFolderOrAsset.getChildren()) {
                if (checkTree(currentTree)) {
                    LOG.debug("allowMatch for tree of type Folder {} Match:: {}", currentTree.getName(), true);
                    return true;
                }
            }
        }

        if (!isFolder(firstParentOfTypeFolderOrAsset)) {
            boolean ret = negate != checkTree(firstParentOfTypeFolderOrAsset);
            LOG.debug("allowMatch for tree of type !Folder && !Asset {} Match:: {}", firstParentOfTypeFolderOrAsset.getName(), true);
            return ret;
        }

        LOG.debug("allowMatch for tree {} Match:: {}", firstParentOfTypeFolderOrAsset.getName(), false);
        return false;
    }

    private boolean isRuleToApplyAnAllow() {
        return !isRuleToApplyADeny();
    }

    private boolean isRuleToApplyADeny() {
        return DENY.equalsIgnoreCase(permissionType);
    }

    @Override
    public boolean matches(String path) {
        return matches();
    }

    @Override
    public boolean matches() {
        return false;
    }

    private boolean denyMatch(Tree tree) {
        // configured property name found on underlying jcr:content node has precedence
        if (hasMetadataAsChild(tree)) {
            boolean match = isMatch(tree);
            return negate != match;
        }
        return false;
    }

    private boolean hasMetadataAsChild(Tree tree) {
        return tree.hasChild(JcrConstants.JCR_CONTENT)
                && tree.getChild(JcrConstants.JCR_CONTENT)
                .hasChild(DamConstants.ACTIVITY_TYPE_METADATA);
    }

    private boolean compareWithOperator(int compareValue) {
        return ("_EQUALS_".equals(operator) && compareValue == 0) ||
                ("_LESS_THAN_EQUALS_".equals(operator) && (compareValue <= 0)) ||
                ("_GREATER_THAN_EQUALS_".equals(operator) && (compareValue >= 0)) ||
                ("_GREATER_THEN_".equals(operator) && (compareValue > 0)) ||
                ("_LESS_THEN_".equals(operator) && (compareValue < 0));
    }

    private boolean isMatch(Tree tree) {
        LOG.debug("isMatch the level");
        return isRequiredLevel(originalTree,level,tree);

    }

    private boolean isRequiredLevel(String OkPath, int level, Tree triggeredTree){
        // Traverse the originalTree up to the specified level
        for (int i = 0; i < level; i++) {
            if (OkPath == null || triggeredTree ==null) {
                return false; // originalTree does not have enough levels, return false
            }
            triggeredTree = triggeredTree.getParent();
        }
        // Check if the triggeredPath is a child of the originalPath
        return OkPath.equals(triggeredTree.getPath());
    }


    @Override
    public boolean equals(Object o) {

        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        if (!(o instanceof SubFolderPattern)) {
            return false;
        }

        SubFolderPattern c = (SubFolderPattern) o;

        // Compare the data members and return accordingly
        return arePermissionTypesEqual(c) &&
                areNegateOperatorsEqual(c) &&
                areOriginalTreesEqual(c) &&
                areOperatorsEqual(c);
    }

    private boolean areOperatorsEqual(SubFolderPattern c) {
        return areTheyBothNull(operator, c.operator) || (operator != null && operator.equals(c.operator));
    }

    private boolean areOriginalTreesEqual(SubFolderPattern c) {
        return areTheyBothNull(originalTree, c.originalTree) || (originalTree != null && originalTree.equals(c.originalTree));
    }


    private boolean areNegateOperatorsEqual(SubFolderPattern c) {
        return areTheyBothNull(negate, c.negate) || negate == c.negate;
    }

    private boolean arePermissionTypesEqual(SubFolderPattern c) {
        return areTheyBothNull(permissionType, c.permissionType) || (permissionType != null && permissionType.equals(c.permissionType));
    }

    @Override
    public int hashCode() {
        return Objects.hash
                (
                        permissionType,
                        negate,
                        originalTree,
                        operator
                );
    }

    private boolean areTheyBothNull(Object o1, Object o2) {
        return o1 == null && o2 == null;
    }
}
