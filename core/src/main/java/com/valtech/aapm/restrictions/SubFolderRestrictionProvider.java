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

import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.security.authorization.restriction.*;
import org.osgi.service.component.annotations.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Component(service = RestrictionProvider.class, immediate = true)
public class SubFolderRestrictionProvider extends AbstractRestrictionProvider {

    private static final String REP_SUB_FOLDER = "rep:subFolder";

    public SubFolderRestrictionProvider() {
        super(supportedRestrictions());
    }

    private static Map<String, RestrictionDefinition> supportedRestrictions() {
        RestrictionDefinition propertyValue = new RestrictionDefinitionImpl(REP_SUB_FOLDER, Type.STRING, false);
        return Collections.singletonMap(propertyValue.getName(), propertyValue);
    }

    //------------------------------------------------< RestrictionProvider >---

    @Override
    public RestrictionPattern getPattern(String oakPath, Tree tree) {
        if (oakPath != null) {
            PropertyState property = tree.getProperty(REP_SUB_FOLDER);
            if (property != null) {
                return HasPropertyValuesPattern.create(property, oakPath);
            }
        }
        return RestrictionPattern.EMPTY;
    }

    @Override
    public RestrictionPattern getPattern(String oakPath, Set<Restriction> restrictions) {
        if (oakPath != null) {
            for (Restriction r : restrictions) {
                String name = r.getDefinition().getName();
                if (REP_SUB_FOLDER.equals(name)) {
                    return HasPropertyValuesPattern.create(r.getProperty(), oakPath);
                }
            }
        }
        return RestrictionPattern.EMPTY;
    }

}
