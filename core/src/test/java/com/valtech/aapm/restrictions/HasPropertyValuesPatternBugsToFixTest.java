/*
 * Copyright 2020 Valtech GmbH
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
import com.google.common.collect.Sets;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.*;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.util.ArrayList;
import java.util.Set;

import static junit.framework.Assert.assertTrue;
import static junitx.framework.Assert.assertFalse;
@Ignore
public class HasPropertyValuesPatternBugsToFixTest {

    public static final String DEFAULT_WORKSPACE_NAME = "test";
    private ContentRepository contentRepository;
    protected ContentSession adminSession;
    protected Root root;
    protected SecurityProvider securityProvider;


    @Before
    public void before() throws Exception {
        Oak myOak = new Oak().with(DEFAULT_WORKSPACE_NAME).with(new OpenSecurityProvider());
        contentRepository = myOak.createContentRepository();
        adminSession = contentRepository.login(new SimpleCredentials("admin", "admin".toCharArray()),"test");
        root = adminSession.getLatestRoot();

        //Repository repo = new Jcr(myOak).createRepository();
        //Session session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()), "test");

        //Repository repo = new Jcr(myOak).createRepository();

        //repo.login()

    }

    //region Bugs to fix
    // With these tests you can reproduce an unexpected behavior
    @Test
    // it is a BUG. Currently it returns true. Problem from "name = propertyValuesWithoutPropertyType.split(operator)[0];"
    public void matches_returns_false_when_negate_deny_rule_on_asset_path_which_owns_single_tag_defined_in_restriction(){
        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("cq:tags", "properties:orientation/portrait", Type.STRING);


        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#string$!cq:tags==properties:orientation/portrait"; // negate deny rule
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertFalse(doesItMatch);
    }

    // adding cases when property is a single one of type int, date...for now for example myProperty == 1 or myProperty > 1 does not work

    @Test
    // it is a BUG. Currently it returns true. Problem from "name = propertyValuesWithoutPropertyType.split(operator)[0];"
    public void matches_returns_false_when_negate_deny_rule_on_asset_path_which_owns_multiple_tags_defined_in_restriction(){
        Set<String> tags = Sets.newHashSet();
        tags.add("properties:orientation/portrait");
        tags.add("properties:orientation/landscape");

        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("cq:tags", tags, Type.STRINGS);


        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#string$!cq:tags==properties:orientation/portrait"; // negate deny rule
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertFalse(doesItMatch);
    }

    @Test
    // it is a BUG. Currently it returns true. Problem comes from type handling is not done for a property of type "single"
    public void matches_returns_true_when_deny_strict_bigger_rule_on_asset_path_which_owns_single_tag_of_type_int_defined_in_restriction(){
        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myNumberProperty", "567", Type.STRING);


        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#int$myNumberProperty>4";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertTrue(doesItMatch);
    }

    @Test
    // it is a BUG. Currently it returns false.
    public void matches_returns_true_when_deny_strict_lower_rule_on_asset_path_which_owns_multiple_tags_of_type_int_defined_in_restriction(){
        //Set<String> tags = Sets.newHashSet();
        ArrayList<String> tags = new ArrayList<>();
        tags.add("14");
        tags.add("13");

        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myNumbers", tags, Type.STRINGS);


        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#int$myNumbers<72";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertTrue(doesItMatch);
    }

    @Test
    // it is a bug. Currently it returns true
    public void matches_returns_false_when_deny_strict_lower_rule_is_false_on_asset_path_which_owns_multiple_tags_of_type_int_defined_in_restriction(){
        Set<String> tags = Sets.newHashSet();
        tags.add("5678");
        tags.add("90");

        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myNumbers", tags, Type.STRINGS);


        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#int$myNumbers<72";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertFalse(doesItMatch);
    }

    @Test
    // it is a bug. Currently it returns false
    public void matches_returns_true_when_deny_strict_bigger_rule_on_asset_path_which_owns_single_tag_of_type_date_defined_in_restriction(){
        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myDate", "2021-05-03T10:09:54.1111+02:00", Type.STRING);


        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#date$myDate>2022-12-08T10:05:57.5946+08:00"; // negate deny rule
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertTrue(doesItMatch);
    }

    @Test
    // : it is a bug : currently it returns true
    public void matches_returns_false_on_asset_path_when_allow_inequality_rule_defined_on_direct_parent_folder_of_this_asset(){
        Tree asset = root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-allow")
                .addChild("Casque_VR_with_tag.jpg");

        asset.setProperty(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSET);
        asset.addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myProperty", "toto");


        //Tree tree = root.getTree("/content/dam/aapm-test/test-allow");
        String propertyValues = "allow#string$!myProperty==toto";
        String originalTree = "/content/dam/aapm-test/test-allow";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(asset, whatever);

        assertFalse(doesItMatch);
    }

    @Test
    // : this is a bug. A recursive child which has an asset which matches => folder should appear. Currently it returns false
    public void matches_returns_true_on_folder_path_when_allow_equality_rule_defined_on_at_least_one_recursive_child_of_this_folder(){
        Tree subfolderContainingGrandSonAsset = root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-allow")
                .addChild("subfolder");
        subfolderContainingGrandSonAsset.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);

        Tree firstSubFolder = subfolderContainingGrandSonAsset.addChild("firstSubFolder"); // This folder is empty
        subfolderContainingGrandSonAsset.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);

        Tree secondSubFolder = subfolderContainingGrandSonAsset.addChild("secondSubFolder");
        subfolderContainingGrandSonAsset.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);

        Tree asset1 = secondSubFolder
                .addChild("Casque_VR_with_tag.jpg");
        asset1.setProperty(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSET);
        asset1.addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myProperty", "noTheGoodValue");

        Tree asset2 = secondSubFolder
                .addChild("Casque2_VR_with_tag.jpg");
        asset2.setProperty(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSET);
        asset2.addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myProperty", "toto");


        //Tree tree = root.getTree("/content/dam/aapm-test/test-allow");
        String propertyValues = "allow#string$myProperty==toto";
        String originalTree = "/content/dam/aapm-test/test-allow/subfolder";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(subfolderContainingGrandSonAsset, whatever);

        assertTrue(doesItMatch);
    }

    //endregion

}