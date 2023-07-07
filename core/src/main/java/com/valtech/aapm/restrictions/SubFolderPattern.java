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

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionPattern;

import java.util.List;
import java.util.stream.IntStream;

public class SubFolderPattern implements RestrictionPattern {
    private final String originalTree;
    private final Integer level;
    private final String permissionType;
    private static final String DENY = "deny";
    List<String> operators = List.of(Operators.EQUALS.getValue(), Operators.GREATER_THAN_EQUALS.getValue(),
            Operators.LESS_THAN_EQUALS.getValue(), Operators.GREATER_THEN.getValue(), Operators.LESS_THEN.getValue());
    private String operator = operators.get(0);
    private final boolean negate;

    SubFolderPattern(String propertyValues, String originalTree) {
        this.originalTree = originalTree;
        permissionType = propertyValues.split("_")[0];
        String propertyValuesWithoutPermissionType = StringUtils.removeStart(propertyValues, permissionType);
        if (propertyValuesWithoutPermissionType.startsWith("!")) {
            negate = true;
        } else {
            negate = false;
        }
        for (int i = 0; i < operators.size(); i++) {
            if (propertyValuesWithoutPermissionType.contains(operators.get(i))) {
                operator = operators.get(i);
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
        boolean match = isMatch(tree);
        return negate != match;
    }

    private boolean isMatch(Tree tree) {
        return isRequiredLevel(originalTree,level,tree,operator);
    }

    /**
     * isRequiredLevel Check either the oakPath is the requested parent according to the given operator
     *
     * @param oakPath
     * @param level
     * @param triggeredTree
     * @param operator
     * @return
     */
    public boolean isRequiredLevel(String oakPath, int level, Tree triggeredTree, String operator) {
        if (oakPath == null || triggeredTree == null || level < 0) {
            return false;
        }
        long descentLevel = -1;
        // Check if the operator requires the descent level to be calculated
        if (operator.equals(Operators.EQUALS.getValue()) || operator.equals(Operators.GREATER_THAN_EQUALS.getValue())
                || operator.equals(Operators.GREATER_THEN.getValue()) || operator.equals(Operators.LESS_THAN_EQUALS.getValue())
                || operator.equals(Operators.LESS_THEN.getValue())) {
            descentLevel = countDescentLevel(oakPath, triggeredTree.getPath());
        }
        // Evaluate the condition based on the operator and level values
        switch (operator) {
            case "_EQUALS_":
                return descentLevel == level;
            case "_GREATER_THAN_EQUALS_":
                return descentLevel >= level;
            case "_GREATER_THEN_":
                return descentLevel > level;
            case "_LESS_THAN_EQUALS_":
                return descentLevel > 0 && descentLevel <= level;
            case "_LESS_THEN_":
                return descentLevel > 0 && descentLevel < level;
            default:
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    }


    /**
     * isTriggeredPathADescendant : Check if oakPath can be the parent of triggeredPath
     *
     * Example :  oakPath = /content/dam/public and triggeredPath = /content/dam/public/test.png must return  true
     * Example :  oakPath = /content/dam/public and triggeredPath = /content/dam/private/test.png must return false
     *
     * @param oakPath
     * @param triggeredPath
     * @return
     */
    public boolean isTriggeredPathADescendant(String oakPath, String triggeredPath){
        return triggeredPath.startsWith(oakPath);
    }

    /**
     *  countDescentLevel : Count the level of descent for a given child
     *
     *   * Example :  oakPath = /content/dam/public and triggeredPath = /content/dam/public/test.png must return 1
     *   * Example :  oakPath = /content/dam/public and triggeredPath = /content/dam/public/parent1/parent2/test.png must return 3
     *
     * @param oakPath
     * @param triggeredPath
     * @return
     */

    public long countDescentLevel(String oakPath, String triggeredPath) {
        if (isTriggeredPathADescendant(oakPath, triggeredPath)) {
            return  IntStream.range(oakPath.length(), triggeredPath.length())
                    .filter(i -> triggeredPath.charAt(i) == '/')
                    .count();
        } else {
            return -1L;
        }
    }
}
