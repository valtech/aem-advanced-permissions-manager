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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.memory.PropertyStates;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.Restriction;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionImpl;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.RestrictionPattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertyValueRestrictionProviderTest {

    private RestrictionImpl restriction;

    private static PropertyState createProperty(String name, String value, Type<String> type) {
        return PropertyStates.createProperty(name, value, type);
    }

    @Test
    void getPattern_returns_empty_pattern_restriction_when_no_restriction_on_targeted_path() {

        PropertyValueRestrictionProvider testedProvider = new PropertyValueRestrictionProvider();
        HashSet<Restriction> noRestriction = new HashSet<>();

        RestrictionPattern computedRestrictionPattern = testedProvider.getPattern("/my/path", noRestriction);

        assertEquals(RestrictionPattern.EMPTY, computedRestrictionPattern);
    }

    @Test
    void getPattern_nominal_case() {
        Type<String> type = Type.STRING;
        String oakPath = "/my/path";
        String name = "hasPropertyValues";
        String matchedConditionPropertyValue = "deny#string$cq:tagsDOUBLE_EQUALSproperties:orientation/portrait";
        boolean isMandatory = false;
        PropertyState property = createProperty(name, matchedConditionPropertyValue, type);
        restriction = new RestrictionImpl(property, isMandatory);
        PropertyValueRestrictionProvider testedProvider = new PropertyValueRestrictionProvider();
        HashSet<Restriction> restrictions = new HashSet<>();
        restrictions.add(restriction);
        HasPropertyValuesPattern expectedRestrictionPattern =
                new HasPropertyValuesPattern(matchedConditionPropertyValue, oakPath);

        RestrictionPattern computedRestrictionPattern = testedProvider.getPattern(oakPath, restrictions);

        assertTrue(computedRestrictionPattern instanceof HasPropertyValuesPattern);
        assertEquals(expectedRestrictionPattern, computedRestrictionPattern);
    }

    @Test
    void getPattern_returns_empty_pattern_restriction_when_special_property_is_a_multiple_one() {
        Type<Iterable<String>> type = Type.STRINGS;
        String oakPath = "/my/path";
        String name = "hasPropertyValues";
        String value1 = "deny#string$cq:tagsDOUBLE_EQUALSproperties:orientation/portrait";
        String value2 = "allow#string$cq:tagsDOUBLE_EQUALSproperties:orientation/landscape";
        List<String> values = new ArrayList<String>();
        values.add(value1);
        values.add(value2);
        boolean isMandatory = false;
        PropertyState property = PropertyStates.createProperty(name, values, Type.STRINGS);
        PropertyValueRestrictionProvider testedProvider = new PropertyValueRestrictionProvider();
        HashSet<Restriction> restrictions = new HashSet<>();
        restriction = new RestrictionImpl(property, isMandatory);
        restrictions.add(restriction);

        RestrictionPattern computedRestrictionPattern = testedProvider.getPattern(oakPath, restrictions);

        assertEquals(RestrictionPattern.EMPTY, computedRestrictionPattern);

    }

    // : create Unit tests for " RestrictionPattern getPattern(String oakPath, Tree tree)"
}
