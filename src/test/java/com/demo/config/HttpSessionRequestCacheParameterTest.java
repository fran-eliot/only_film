package com.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpSessionRequestCacheParameterTest {

    @Test
    void nullMatchingParameterAllowsSavedRequestWithoutContinueQueryParameter() {
        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        cache.setMatchingRequestParameterName(null);

        MockHttpServletRequest original = new MockHttpServletRequest("GET", "/tickets");
        cache.saveRequest(original, new MockHttpServletResponse());

        MockHttpServletRequest revisited = new MockHttpServletRequest("GET", "/tickets");
        revisited.setSession((MockHttpSession) original.getSession());

        assertNotNull(cache.getMatchingRequest(revisited, new MockHttpServletResponse()));
    }

    @Test
    void defaultMatchingParameterRequiresContinueQueryParameter() {
        HttpSessionRequestCache cache = new HttpSessionRequestCache();

        MockHttpServletRequest original = new MockHttpServletRequest("GET", "/tickets");
        cache.saveRequest(original, new MockHttpServletResponse());

        MockHttpServletRequest revisited = new MockHttpServletRequest("GET", "/tickets");
        revisited.setSession((MockHttpSession) original.getSession());

        assertNull(cache.getMatchingRequest(revisited, new MockHttpServletResponse()));
    }
}
