package com.valtech.aapm.restrictions;

import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.*;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.SimpleCredentials;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SubFolderPatternTest {

    static final String DEFAULT_WORKSPACE_NAME = "test";
    public static final String CONTENT_DAM_PUBLIC = "/content/dam/public";
    private ContentRepository contentRepository;
    protected ContentSession adminSession;
    protected Root root;
    protected SecurityProvider securityProvider;


    @BeforeEach
    void before() throws Exception {
        Oak myOak = new Oak().with(DEFAULT_WORKSPACE_NAME).with(new OpenSecurityProvider());
        contentRepository = myOak.createContentRepository();
        adminSession = contentRepository.login(new SimpleCredentials("admin", "admin".toCharArray()), "test");
        root = adminSession.getLatestRoot();
    }
    private SubFolderPattern fixture ;

    @Test
    void testIsTriggeredPathADescendant() {

        String propertyValues = "deny_GREATER_THAN_EQUALS_2";
        String originalTree = "/content/dam/public";

        SubFolderPattern subFolderPattern = new SubFolderPattern(propertyValues, originalTree) ;

        // Test when the triggeredPath is a descendant of the oakPath
        String triggeredPath = "/content/dam/public/photo.jpg";
        boolean result = subFolderPattern.isTriggeredPathADescendant(originalTree, triggeredPath);
        assertTrue(result);

        // Test when the triggeredPath is not a descendant of the oakPath
        triggeredPath = "/content/assets/images/logo.png";
        result = subFolderPattern.isTriggeredPathADescendant(originalTree, triggeredPath);
        assertFalse(result);
    }

    @Test
    void testCountDescentLevel() {

        String propertyValues = "deny_GREATER_THAN_EQUALS_2";
        String originalTree = "/content/dam/public";
        SubFolderPattern subFolderPattern = new SubFolderPattern(propertyValues, originalTree) ;

        // Test when the triggeredPath is a descendant of the oakPath
        String oakPath = "/content/dam/public";
        String triggeredPath = "/content/dam/public/photo.jpg";
        String otherTriggeredPath = "/content/dam/public/parent1/parent2/photo.jpg" ;
        long result = subFolderPattern.countDescentLevel(oakPath, triggeredPath);
        long result2 = subFolderPattern.countDescentLevel(oakPath, otherTriggeredPath);
        long resultEqualPath = subFolderPattern.countDescentLevel(oakPath, oakPath);
        assertEquals(1, result);
        assertEquals(3, result2);
        assertEquals(0, resultEqualPath);

        // Test when the triggeredPath is not a descendant of the oakPath
        oakPath = "/content/dam";
        triggeredPath = "/content/assets/images/logo.png";
        result = subFolderPattern.countDescentLevel(oakPath, triggeredPath);
        assertEquals(-1, result);
    }

    @Test
    void isRequiredLevel() {
        root.getTree("/").addChild("content").addChild("dam").addChild("public").addChild("parent1").addChild("parent2").addChild("asset.jpg");
        Tree parent1Tree = root.getTree("/content/dam/public/parent1");
        Tree parent2Tree = root.getTree("/content/dam/public/parent1/parent2");
        Tree parent3Tree = root.getTree("/content/dam/public/parent1/parent2/asset.jpg");
        String propertyValues = "deny_LESS_THAN_EQUALS_2";
        String originalTree = "/content/dam/public";
        SubFolderPattern subFolderPattern = new SubFolderPattern(propertyValues, originalTree) ;

        //_LESS_THAN_EQUALS_ operations
        assertTrue(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,2,parent1Tree,"_LESS_THAN_EQUALS_"));
        assertTrue(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,2,parent2Tree,"_LESS_THAN_EQUALS_"));
        assertFalse(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,2,parent3Tree,"_LESS_THAN_EQUALS_"));
        assertTrue(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,3,parent3Tree,"_LESS_THAN_EQUALS_"));

        //_LESS_THAN_EQUALS_ operations
        assertTrue(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,2,parent1Tree,"_LESS_THEN_"));
        assertFalse(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,2,parent2Tree,"_LESS_THEN_"));
        assertFalse(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,3,parent3Tree,"_LESS_THEN_"));

        //_GREATER_THEN_ operations
        assertFalse(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,2,parent1Tree,"_GREATER_THEN_"));
        assertFalse(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,2,parent2Tree,"_GREATER_THEN_"));
        assertTrue(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,2,parent3Tree,"_GREATER_THEN_"));
        assertFalse(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,3,parent3Tree,"_GREATER_THEN_"));

        //_GREATER_THAN_EQUALS_ operations
        assertFalse(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,2,parent1Tree,"_GREATER_THAN_EQUALS_"));
        assertTrue(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,2,parent2Tree,"_GREATER_THAN_EQUALS_"));
        assertTrue(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,2,parent3Tree,"_GREATER_THAN_EQUALS_"));
        assertTrue(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,3,parent3Tree,"_GREATER_THAN_EQUALS_"));

        //_EQUALS_ operations
        assertFalse(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,2,parent1Tree,"_EQUALS_"));
        assertTrue(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,2,parent2Tree,"_EQUALS_"));
        assertFalse(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,2,parent3Tree,"_EQUALS_"));

        //ERROR operations
        assertFalse(subFolderPattern.isRequiredLevel(null,2,parent1Tree,"_EQUALS_"));
        assertFalse(subFolderPattern.isRequiredLevel(CONTENT_DAM_PUBLIC,2,null,"_EQUALS_"));
        assertFalse(subFolderPattern.isRequiredLevel(null,2,null,null));


    }
}