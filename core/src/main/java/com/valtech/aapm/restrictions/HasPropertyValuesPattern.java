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
import com.drew.lang.Iterables;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionPattern;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class HasPropertyValuesPattern implements RestrictionPattern {

    private static final Logger LOG = LoggerFactory.getLogger(HasPropertyValuesPattern.class);
    private final String originalTree;
    private final String name;
    private final List<String> statusList;
    private final String permissionType;
    private final String propertyType;
    private static final String DENY = "deny";

    List<String> operators = List.of("_EQUALS_", "_GREATER_THAN_EQUALS_", "_LESS_THAN_EQUALS_", "_GREATER_THEN_", "_LESS_THEN_");
    private String operator = operators.get(0);

    private final boolean negate;

    HasPropertyValuesPattern(String propertyValues, String originalTree) {
        this.originalTree = originalTree;

        // allow_property=test
        // allow_date_property=01/10/2021

        permissionType = propertyValues.split("_")[0];
        String propertyValuesWithoutPermissionType = StringUtils.removeStart(propertyValues, permissionType + "_");

        propertyType = propertyValues.split("_")[1];
        String propertyValuesWithoutPropertyType = StringUtils.removeStart(propertyValuesWithoutPermissionType, propertyType + "_");

        if (propertyValuesWithoutPropertyType.startsWith("!")) {
            negate = true;
        } else {
            negate = false;
        }
        for (int i = 0; i < operators.size(); i++) {
            if (propertyValuesWithoutPropertyType.contains(operators.get(i))) {
                operator = operators.get(i);
                break;
            }
        }

        name = propertyValuesWithoutPropertyType.split(operator)[0];
        String values = propertyValuesWithoutPropertyType.split(operator)[1];
        String[] status = values.split(",");
        statusList = Arrays.asList(status);
    }

    static RestrictionPattern create(PropertyState stringProperty, String originalTree) {
        if (stringProperty.count() == 1) {
            return new HasPropertyValuesPattern(stringProperty.getValue(Type.STRING), originalTree);
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
        if (parent.getPath().equals("/content/dam")) {
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
        if (parent.getPath().equals("/content/dam")) {
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
            return isMatch(tree);
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
            return allowMatch(tree);
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
        Tree metadataNode = tree.getChild(JcrConstants.JCR_CONTENT).getChild(DamConstants.ACTIVITY_TYPE_METADATA);
        if (!isRestrictionPropertyExisting(metadataNode)) {
            LOG.debug("isMatch::!isRestrictionPropertyExisting(metadataNode) metadataNode:: {}, Return:: {}", metadataNode, false);
            return false;
        }
        if (isMetadataPropertyAMultipleValuesOne(metadataNode)) {
            boolean ret = doesItMatchForMultipleProperty(metadataNode);
            LOG.debug("isMatch::isMetadataPropertyAMultipleValuesOne(metadataNode) metadataNode:: {}, Return:: {}", metadataNode, ret);
            return ret;
        }
        if (isMetadataPropertyASingleValueOne(metadataNode)) {
            boolean ret = doesItMatchForSingleProperty(metadataNode);
            LOG.debug("isMatch::isMetadataPropertyASingleValueOne(metadataNode) metadataNode:: {}, Return:: {}", metadataNode, ret);
            return ret;
        }
        LOG.debug("isMatch metadataNode:: {}, Return:: {} (default)", metadataNode, false);
        return false;
    }

    private boolean isRestrictionPropertyExisting(Tree metadataNode) {
        return metadataNode.hasProperty(name);
    }

    private boolean isMetadataPropertyASingleValueOne(Tree metadataNode) {
        return !isMetadataPropertyAMultipleValuesOne(metadataNode);
    }

    private boolean doesItMatchForSingleProperty(Tree metadataNode) {
        return statusList.contains(metadataNode.getProperty(name).getValue(Type.STRING));
    }

    private boolean doesItMatchForMultipleProperty(Tree metadataNode) {
        boolean match = false;
        List<String> propertyValues = Iterables.toList(metadataNode.getProperty(name).getValue(Type.STRINGS));
        for (int i = 0; i < propertyValues.size() && !match; i++) {
            String value = propertyValues.get(i);
            if (isPropertyTypeAString()) {
                match = doesItMatchForAStringProperty(value);
            }
            if (isPropertyTypeAnInt()) {
                match = doesItMatchForAnIntProperty(value);
            }
            if (isPropertyTypeADate()) {
                match = doesItMatchForADateProperty(value);
            }
        }
        return match;
    }

    private boolean doesItMatchForADateProperty(String value) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            Date dateProperty = format.parse(value);
            Date valueProperty = new Date();
            if (!"today".equalsIgnoreCase(statusList.get(0))) {
                valueProperty = format.parse(statusList.get(0));
            }

            int compare = valueProperty.compareTo(dateProperty);
            return compareWithOperator(compare);

        } catch (ParseException e) {
            // Because it is not a date => not possible to compare => false
        }
        return false;
    }

    private boolean doesItMatchForAnIntProperty(String value) {
        try {
            Integer intProperty = Integer.parseInt(value);
            Integer valueProperty = Integer.parseInt(statusList.get(0));

            int compare = valueProperty.compareTo(intProperty);
            return compareWithOperator(compare);

        } catch (Exception e) {
            // Because it is not an int => not possible to compare => false
        }
        return false;
    }

    private boolean doesItMatchForAStringProperty(String value) {
        return statusList.contains(value);
    }

    private boolean isPropertyTypeADate() {
        return propertyType.equals("date");
    }

    private boolean isPropertyTypeAnInt() {
        return propertyType.equals("int");
    }

    private boolean isPropertyTypeAString() {
        return propertyType.equals("string");
    }

    private boolean isMetadataPropertyAMultipleValuesOne(Tree metadataNode) {
        return metadataNode.hasProperty(name) && metadataNode.getProperty(name).isArray();
    }

    @Override
    public boolean equals(Object o) {

        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        if (!(o instanceof HasPropertyValuesPattern)) {
            return false;
        }

        HasPropertyValuesPattern c = (HasPropertyValuesPattern) o;

        // Compare the data members and return accordingly
        return arePermissionTypesEqual(c) &&
                areNegateOperatorsEqual(c) &&
                areNamesEqual(c) &&
                areStatusListsEqual(c) &&
                areOriginalTreesEqual(c) &&
                arePropertyTypesEqual(c) &&
                areOperatorsEqual(c);
    }

    private boolean areOperatorsEqual(HasPropertyValuesPattern c) {
        return areTheyBothNull(operator, c.operator) || (operator != null && operator.equals(c.operator));
    }

    private boolean arePropertyTypesEqual(HasPropertyValuesPattern c) {
        return areTheyBothNull(propertyType, c.propertyType) || (propertyType != null && propertyType.equals(c.propertyType));
    }

    private boolean areOriginalTreesEqual(HasPropertyValuesPattern c) {
        return areTheyBothNull(originalTree, c.originalTree) || (originalTree != null && originalTree.equals(c.originalTree));
    }

    private boolean areStatusListsEqual(HasPropertyValuesPattern c) {
        return areTheyBothNull(statusList, c.statusList) || (statusList != null && statusList.equals(c.statusList));
    }

    private boolean areNamesEqual(HasPropertyValuesPattern c) {
        return areTheyBothNull(name, c.name) || (name != null && name.equals(c.name));
    }

    private boolean areNegateOperatorsEqual(HasPropertyValuesPattern c) {
        return areTheyBothNull(negate, c.negate) || negate == c.negate;
    }

    private boolean arePermissionTypesEqual(HasPropertyValuesPattern c) {
        return areTheyBothNull(permissionType, c.permissionType) || (permissionType != null && permissionType.equals(c.permissionType));
    }

    @Override
    public int hashCode() {
        return Objects.hash
                (
                        permissionType,
                        negate,
                        name,
                        statusList,
                        originalTree,
                        propertyType,
                        operator
                );
    }

    private boolean areTheyBothNull(Object o1, Object o2) {
        return o1 == null && o2 == null;
    }
}
