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
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.junit.Before;
import org.junit.Test;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.util.ArrayList;
import java.util.Set;
import static junit.framework.Assert.assertTrue;
import static junitx.framework.Assert.assertFalse;

public class HasPropertyValuesPatternTest {

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

    //region State of art
    // Actual tested behaviors

    //region Deny cases
    @Test
    public void matches_returns_false_when_deny_rule_on_a_not_asset(){
        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny");

        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#string$cq:tags==properties:orientation/portrait";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;


        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertFalse(doesItMatch);
    }


    @Test
    public void matches_returns_false_when_deny_rule_on_a_not_asset_node_containing_jcr_node_child(){
        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT);

        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#string$cq:tags==properties:orientation/portrait";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;


        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertFalse(doesItMatch);
    }

    @Test
    public void matches_returns_false_when_negate_deny_rule_on_a_not_asset_path(){
        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny");

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
    public void matches_returns_false_when_deny_rule_on_asset_path_which_does_not_have_the_special_property(){
        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("aPropertyNotUsefulToApplyRestriction", "whatever");


        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#string$cq:tags==properties:orientation/portrait";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertFalse(doesItMatch);
    }

    @Test
    public void matches_returns_true_when_negate_deny_rule_on_asset_path_which_does_not_have_the_special_property(){
        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("aPropertyNotUsefulToApplyRestriction", "whatever");


        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#string$!cq:tags==properties:orientation/portrait"; // negate deny rule
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertTrue(doesItMatch);
    }

    @Test
    public void matches_returns_true_when_deny_rule_on_asset_path_which_owns_multiple_tags_of_type_string_defined_in_restriction(){
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
        String propertyValues = "deny#string$cq:tags==properties:orientation/portrait";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertTrue(doesItMatch);
    }

    @Test
    public void matches_returns_true_when_deny_equality_rule_on_asset_path_which_owns_multiple_tags_of_type_int_defined_in_restriction(){
        Set<String> tags = Sets.newHashSet();
        tags.add("1");
        tags.add("75");

        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myNumbers", tags, Type.STRINGS);


        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#int$myNumbers==75";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertTrue(doesItMatch);
    }

    @Test
    public void matches_returns_false_when_deny_equality_rule_is_false_on_asset_path_which_owns_multiple_tags_of_type_int_defined_in_restriction(){
        Set<String> tags = Sets.newHashSet();
        tags.add("13");
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
        String propertyValues = "deny#int$myNumbers==72";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertFalse(doesItMatch);
    }

    @Test
    public void matches_returns_false_when_deny_rule_on_int_checked_for_a_not_int(){
        Set<String> tags = Sets.newHashSet();
        tags.add("NaN");

        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myNumbers", tags, Type.STRINGS);

        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#int$myNumbers==72";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertFalse(doesItMatch);
    }

    @Test
    public void matches_returns_false_when_deny_rule_on_date_checked_for_a_not_date(){
        Set<String> tags = Sets.newHashSet();
        tags.add("NotADate");

        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myDate", tags, Type.STRINGS);

        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#date$myDate==2022-03-01'T'20:07:11.000000";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertFalse(doesItMatch);
    }

    @Test
    public void matches_returns_false_when_deny_equality_rule_is_applied_on_a_not_asset(){
        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny");

        Tree notAnAsset = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#int$myNumbers==72";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(notAnAsset, whatever);

        assertFalse(doesItMatch);
    }

    @Test
    public void matches_returns_true_when_deny_strict_bigger_rule_on_asset_path_which_owns_multiple_tags_of_type_int_defined_in_restriction(){
        Set<String> tags = Sets.newHashSet();
        tags.add("13");
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
        String propertyValues = "deny#int$myNumbers>72";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertTrue(doesItMatch);
    }

    @Test
    public void matches_returns_false_when_deny_strict_bigger_rule_is_false_on_asset_path_which_owns_multiple_tags_of_type_int_defined_in_restriction(){
        Set<String> tags = Sets.newHashSet();
        tags.add("83");
        tags.add("72");

        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myNumbers", tags, Type.STRINGS);


        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#int$myNumbers>72";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertFalse(doesItMatch);
    }

    @Test
    public void matches_returns_true_when_deny_rule_on_asset_path_which_owns_single_tag_of_type_string_defined_in_restriction(){
        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("cq:tags", "properties:orientation/portrait", Type.STRING);


        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#string$cq:tags==properties:orientation/portrait"; // negate deny rule
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertTrue(doesItMatch);
    }

    @Test
    public void matches_returns_true_when_deny_equality_rule_on_asset_path_which_owns_single_tag_of_type_int_defined_in_restriction(){
        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myNumberProperty", "4", Type.STRING);


        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#int$myNumberProperty==4";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertTrue(doesItMatch);
    }

    @Test
    public void matches_returns_true_when_deny_strict_lower_rule_on_asset_path_which_owns_single_tag_of_type_int_defined_in_restriction(){
        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myNumberProperty", "567", Type.STRING);


        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#int$myNumberProperty<4";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertFalse(doesItMatch);
    }

    @Test
    public void matches_returns_true_when_deny_rule_on_asset_path_which_owns_single_tag_of_type_date_defined_in_restriction(){
        root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-deny")
                .addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myDate", "2022-12-08T10:05:57.5946+08:00", Type.STRING);


        Tree tree = root.getTree("/content/dam/aapm-test/test-deny");
        String propertyValues = "deny#date$myDate==2022-12-08T10:05:57.5946+08:00"; // negate deny rule
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(tree, whatever);

        assertTrue(doesItMatch);
    }

     // : adding cases when property is a single one of type int, date...for now for example myProperty == 1 or myProperty > 1 does not work

    //endregion

    //region Allow cases
    @Test
    public void matches_returns_true_on_asset_path_when_defined_allow_equality_rule_is_true_on_direct_parent_folder_of_this_asset(){
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
        String propertyValues = "allow#string$myProperty==toto";
        String originalTree = "/content/dam/aapm-test/test-allow";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(asset, whatever);

        assertTrue(doesItMatch);
    }

    @Test
    public void matches_returns_true_on_folder_path_when_defined_allow_equality_rule_is_true_on_at_least_one_child_of_this_folder(){
        Tree subfolderContainingAssets = root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-allow")
                .addChild("subfolder");
        subfolderContainingAssets.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);

        Tree asset1 = subfolderContainingAssets
                .addChild("Casque_VR_with_tag.jpg");
        asset1.setProperty(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSET);
        asset1.addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myProperty", "noTheGoodValue");

        Tree asset2 = subfolderContainingAssets
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

        boolean doesItMatch = hasPropertyValuesPattern.matches(subfolderContainingAssets, whatever);

        assertTrue(doesItMatch);
    }

    @Test
    public void matches_returns_false_on_folder_path_when_defined_allow_equality_rule_is_false_for_all_children_of_this_folder(){
        Tree subfolderContainingAssets = root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("pbop-hackathon-2021")
                .addChild("test-allow")
                .addChild("subfolder");
        subfolderContainingAssets.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);

        Tree asset1 = subfolderContainingAssets
                .addChild("Casque_VR_with_tag.jpg");
        asset1.setProperty(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSET);
        asset1.addChild(JcrConstants.JCR_CONTENT)
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myProperty", "noTheGoodValue");

        Tree notAssetNode = subfolderContainingAssets
                .addChild("notAssetNode");
        notAssetNode.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_BASE);

        //Tree tree = root.getTree("/content/dam/pbop-hackathon-2021/test-allow");
        String propertyValues = "allow#string$myProperty==toto";
        String originalTree = "/content/dam/pbop-hackathon-2021/test-allow/subfolder";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(subfolderContainingAssets, whatever);

        assertFalse(doesItMatch);
    }


    @Test
    public void matches_returns_true_on_asset_rendition_node_when_defined_allow_equality_rule_is_true_on_direct_parent_folder_containing_the_asset(){
        Tree asset = root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-allow")
                .addChild("Casque_VR_with_tag.jpg");
        asset.setProperty(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSET);
        Tree jcrContentOfAsset = asset.addChild(JcrConstants.JCR_CONTENT);
        jcrContentOfAsset.setProperty(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSETCONTENT);
        jcrContentOfAsset
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myProperty", "toto");

        asset.getChild(JcrConstants.JCR_CONTENT)
                .addChild("renditions")
                .setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);

        asset.getChild(JcrConstants.JCR_CONTENT)
                .getChild("renditions")
                .addChild("cq5dam.thumbnail.140.100.png")
                .setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE);
        Tree testedTree = root.getTree("/content/dam/aapm-test/test-allow/Casque_VR_with_tag.jpg/" +
                "jcr:content/renditions/cq5dam.thumbnail.140.100.png");
        String propertyValues = "allow#string$myProperty==toto";
        String originalTree = "/content/dam/aapm-test/test-allow";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(testedTree, whatever);

        assertTrue(doesItMatch);
    }

    @Test
    public void matches_returns_true_on_asset_rendition_node_when_defined_allow_equality_rule_is_true_on_ancestor_parent_folder_containing_the_asset(){
        Tree subfolderContainingAsset = root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-allow")
                .addChild("subfolder");
        subfolderContainingAsset.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
        Tree asset = subfolderContainingAsset
                .addChild("Casque_VR_with_tag.jpg");
        asset.setProperty(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSET);
        Tree jcrContentOfAsset = asset.addChild(JcrConstants.JCR_CONTENT);
        jcrContentOfAsset.setProperty(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSETCONTENT);
        jcrContentOfAsset
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myProperty", "toto");

        asset.getChild(JcrConstants.JCR_CONTENT)
                .addChild("renditions")
                .setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);

        asset.getChild(JcrConstants.JCR_CONTENT)
                .getChild("renditions")
                .addChild("cq5dam.thumbnail.140.100.png")
                .setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE);
        Tree testedTree = root.getTree("/content/dam/aapm-test/test-allow/subfolder/Casque_VR_with_tag.jpg/" +
                "jcr:content/renditions/cq5dam.thumbnail.140.100.png");
        String propertyValues = "allow#string$myProperty==toto";
        String originalTree = "/content/dam/aapm-test/test-allow";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(testedTree, whatever);

        assertTrue(doesItMatch);
    }

    @Test
    public void matches_returns_true_on_asset_rendition_node_when_defined_allow_equality_rule_is_true_on_ancestor_parent_sling_ordered_folder_containing_the_asset(){
        Tree subfolderContainingAsset = root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-allow")
                .addChild("subfolder");
        subfolderContainingAsset.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
        Tree asset = subfolderContainingAsset
                .addChild("Casque_VR_with_tag.jpg");
        asset.setProperty(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSET);
        Tree jcrContentOfAsset = asset.addChild(JcrConstants.JCR_CONTENT);
        jcrContentOfAsset.setProperty(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSETCONTENT);
        jcrContentOfAsset
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myProperty", "toto");

        asset.getChild(JcrConstants.JCR_CONTENT)
                .addChild("renditions")
                .setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_ORDERED_FOLDER);

        asset.getChild(JcrConstants.JCR_CONTENT)
                .getChild("renditions")
                .addChild("cq5dam.thumbnail.140.100.png")
                .setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE);
        Tree testedTree = root.getTree("/content/dam/aapm-test/test-allow/subfolder/Casque_VR_with_tag.jpg/" +
                "jcr:content/renditions/cq5dam.thumbnail.140.100.png");
        String propertyValues = "allow#string$myProperty==toto";
        String originalTree = "/content/dam/aapm-test/test-allow";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(testedTree, whatever);

        assertTrue(doesItMatch);
    }

    @Test
    public void matches_returns_true_on_asset_rendition_node_when_defined_allow_equality_rule_is_true_on_ancestor_parent_sling_folder_containing_the_asset(){
        Tree subfolderContainingAsset = root.getTree("/")
                .addChild("content")
                .addChild("dam")
                .addChild("aapm-test")
                .addChild("test-allow")
                .addChild("subfolder");
        subfolderContainingAsset.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
        Tree asset = subfolderContainingAsset
                .addChild("Casque_VR_with_tag.jpg");
        asset.setProperty(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSET);
        Tree jcrContentOfAsset = asset.addChild(JcrConstants.JCR_CONTENT);
        jcrContentOfAsset.setProperty(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSETCONTENT);
        jcrContentOfAsset
                .addChild(DamConstants.ACTIVITY_TYPE_METADATA)
                .setProperty("myProperty", "toto");

        asset.getChild(JcrConstants.JCR_CONTENT)
                .addChild("renditions")
                .setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_FOLDER);

        asset.getChild(JcrConstants.JCR_CONTENT)
                .getChild("renditions")
                .addChild("cq5dam.thumbnail.140.100.png")
                .setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE);
        Tree testedTree = root.getTree("/content/dam/aapm-test/test-allow/subfolder/Casque_VR_with_tag.jpg/" +
                "jcr:content/renditions/cq5dam.thumbnail.140.100.png");
        String propertyValues = "allow#string$myProperty==toto";
        String originalTree = "/content/dam/aapm-test/test-allow";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches(testedTree, whatever);

        assertTrue(doesItMatch);
    }


    //endregion

    @Test
    public void equals_returns_false_if_at_least_one_internal_field_is_different(){
        Session session = null;

        String propertyValues = "allow#string$myProperty==toto";
        String originalTree = "/content/dam/pbop-hackathon-2021/test-allow";
        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        String propertyValues2 = "deny#string$myProperty==toto";
        String originalTree2 = "/content/dam/pbop-hackathon-2021/test-allow";
        HasPropertyValuesPattern hasPropertyValuesPattern2 = new HasPropertyValuesPattern(propertyValues2, originalTree2, session);

        assertFalse(hasPropertyValuesPattern.equals(hasPropertyValuesPattern2));



    }

    @Test
    public void matches_returns_false(){
        String propertyValues = "deny#string$cq:tags==properties:orientation/portrait";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        PropertyState whatever = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches();

        assertFalse(doesItMatch);
    }

    @Test
    public void matches_returns_false_for_whatever_path(){
        String propertyValues = "deny#string$cq:tags==properties:orientation/portrait";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;

        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        boolean doesItMatch = hasPropertyValuesPattern.matches("/whatever/path");

        assertFalse(doesItMatch);
    }

    @Test
    public void equals_returns_true_if_same_reference(){
        String propertyValues = "deny#string$cq:tags==properties:orientation/portrait";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);

        assertTrue(hasPropertyValuesPattern.equals(hasPropertyValuesPattern));
    }

    @Test
    public void equals_returns_false_for_objects_of_different_types(){
        String propertyValues = "deny#string$cq:tags==properties:orientation/portrait";
        String originalTree = "/content/dam/aapm-test/test-deny";
        Session session = null;
        HasPropertyValuesPattern hasPropertyValuesPattern = new HasPropertyValuesPattern(propertyValues, originalTree, session);
        String nothingSpecial = "test";

        assertFalse(hasPropertyValuesPattern.equals(nothingSpecial));
    }

    //endregion

}