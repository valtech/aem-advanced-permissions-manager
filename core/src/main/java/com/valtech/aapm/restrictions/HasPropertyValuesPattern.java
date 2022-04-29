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
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionPattern;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class HasPropertyValuesPattern implements RestrictionPattern {

    private final String originalTree;
    private final String name;
    private final String values;
    private final String[] status;
    private final List<String> statusList;
    private final String permissionType;
    private final String propertyType;
    private static final String DENY = "deny";

    List<String> operatorList = List.of("==", ">=", "<=","<",">");
    private String operator = "==";

    private final boolean negate;

    HasPropertyValuesPattern(String propertyValues,String originalTree) {
        this.originalTree = originalTree;

        // allow#property=test
        // allow#date$property=01/10/2021

        permissionType = propertyValues.split("#")[0] ;
        String propertyValuesWithoutPermissionType = propertyValues.split("#")[1];

        propertyType = propertyValuesWithoutPermissionType.split("\\$")[0];
        String propertyValuesWithoutPropertyType = propertyValuesWithoutPermissionType.split("\\$")[1];


        if (propertyValuesWithoutPropertyType.startsWith("!")) {
            negate = true;
        } else {
            negate = false;
        }
        for(int i = 0;i<operatorList.size();i++){
            if(propertyValuesWithoutPropertyType.contains(operatorList.get(i))){
                operator = operatorList.get(i);
                break;
            }
        }

        name = propertyValuesWithoutPropertyType.split(operator)[0];
        values = propertyValuesWithoutPropertyType.split(operator)[1];
        status = values.split(",");
        statusList = Arrays.asList(status);


    }

    static RestrictionPattern create(PropertyState stringProperty,String originalTree) {
        if (stringProperty.count() == 1) {
            return new HasPropertyValuesPattern(stringProperty.getValue(Type.STRING),originalTree);
        } else {
            return RestrictionPattern.EMPTY;
        }
    }

    private Tree getParentAssetNode(Tree tree){
        Tree parent = tree.getParent();
        if(isTooHighLevelInHierarchy(parent)) return null;
        if(isAsset(parent)) return parent;
        return getParentAssetNode(parent);
    }

    private boolean isTooHighLevelInHierarchy(Tree parent) {
        return originalTree.contains(parent.getPath());
    }

    private Tree getFirstParentOfTypeFolderOrAsset(Tree tree){
        if(!isFolder(tree) && !isAsset(tree)) return getFirstParentOfTypeFolderOrAsset(tree.getParent());
        return tree;
    }

    private boolean isAsset(Tree tree) {
        String type = tree.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING);
        return DamConstants.NT_DAM_ASSET.equalsIgnoreCase(type);
    }

    private boolean isFolder(Tree tree) {
        String type = tree.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue(Type.STRING);
        return JcrResourceConstants.NT_SLING_ORDERED_FOLDER.equalsIgnoreCase(type) 
                || JcrConstants.NT_FOLDER.equalsIgnoreCase(type) 
                || JcrResourceConstants.NT_SLING_FOLDER.equalsIgnoreCase(type) ;
    }


    private boolean checkTree(Tree tree){
        if (hasMetadataFolder(tree)) return isMatch(tree);
        return false;
    }

    private boolean hasMetadataFolder(Tree tree) {
        return tree.hasChild(JcrConstants.JCR_CONTENT)
                && tree.getChild(JcrConstants.JCR_CONTENT)
                .hasChild(DamConstants.ACTIVITY_TYPE_METADATA);
    }

    @Override
    public boolean matches(Tree tree, PropertyState propertyState) {
        if(isRuleToApplyADeny())    return denyMatch(tree);
        if(isRuleToApplyAnAllow())  return allowMatch(tree);
        return false;
    }

    private boolean allowMatch(Tree tree) {
        Tree firstParentOfTypeFolderOrAsset = getFirstParentOfTypeFolderOrAsset(tree);
        Tree firstParentOfTypeAsset = getParentAssetNode(firstParentOfTypeFolderOrAsset);
        if(exists(firstParentOfTypeAsset)) // There is a parent asset handled by restriction rule
            return negate != checkTree(firstParentOfTypeAsset);

        if(isFolder(firstParentOfTypeFolderOrAsset)){
            for(Tree currentTree : firstParentOfTypeFolderOrAsset.getChildren()){
                if(checkTree(currentTree)){
                    return true;
                }
            }
        }

        if(!isFolder(firstParentOfTypeFolderOrAsset))
            return negate != checkTree(firstParentOfTypeFolderOrAsset);

        return false;
    }

    private boolean exists(Tree assetTree) {
        return assetTree != null;
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

    private boolean denyMatch(Tree tree){
        // configured property name found on underlying jcr:content node has precedence
        if (hasMetadataAsChild(tree)) {
            boolean match = isMatch(tree);
            return negate != match;
        }
        return false ;
    }

    private boolean hasMetadataAsChild(Tree tree) {
        return tree.hasChild(JcrConstants.JCR_CONTENT)
                && tree.getChild(JcrConstants.JCR_CONTENT)
                    .hasChild(DamConstants.ACTIVITY_TYPE_METADATA);
    }

    private boolean compareWithOperator(int compareValue){
        return  ("==".equals(operator) && compareValue == 0) ||
                ("<=".equals(operator) && (compareValue <= 0)) ||
                (">=".equals(operator) && (compareValue >= 0)) ||
                (">".equals(operator) && (compareValue > 0)) ||
                ("<".equals(operator) && (compareValue < 0));
    }

    private boolean isMatch(Tree tree) {
        Tree metadataNode = tree.getChild(JcrConstants.JCR_CONTENT).getChild(DamConstants.ACTIVITY_TYPE_METADATA);
        if(!isRestrictionPropertyExisting(metadataNode))        return false;
        if(isMetadataPropertyAMultipleValuesOne(metadataNode))  return doesItMatchForMultipleProperty(metadataNode);
        if(isMetadataPropertyASingleValueOne(metadataNode))     return doesItMatchForSingleProperty(metadataNode);
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
        for(int i=0 ; i < propertyValues.size() && !match ; i++){
            String value = propertyValues.get(i);
            if(isPropertyTypeAString()) match = doesItMatchForAStringProperty(value);
            if(isPropertyTypeAnInt())   match = doesItMatchForAnIntProperty(value);
            if(isPropertyTypeADate())   match = doesItMatchForADateProperty(value);
        }
        return match;
    }

    private boolean doesItMatchForADateProperty(String value) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            Date dateProperty = format.parse(value);
            Date valueProperty = new Date();
            if(!"today".equalsIgnoreCase(statusList.get(0))){
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

        }catch(Exception e) {
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
        return  arePermissionTypesEqual(c) &&
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

    private boolean areTheyBothNull(Object o1, Object o2){
        return o1 == null && o2 == null;
    }
}