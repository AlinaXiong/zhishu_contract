package com.hero.middleware.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZhiShuSynServiceCounterPartyCodeTest {

    @Test
    void preservesSemicolonJoinedCodesFromExcel() {
        ZhiShuSynServiceImpl service = new ZhiShuSynServiceImpl("unused.xlsx", 1);

        @SuppressWarnings("unchecked")
        Set<String> actual = (Set<String>) ReflectionTestUtils.invokeMethod(
                service, "splitCounterPartyCodes", " V001;C001, V002\nV003；C003 ");

        assertEquals(new LinkedHashSet<>(Arrays.asList("V001;C001", "V002", "V003；C003")), actual);
    }

    @Test
    void doesNotTransformCounterPartyCodes() {
        ZhiShuSynServiceImpl service = new ZhiShuSynServiceImpl("unused.xlsx", 1);
        Set<String> sourceCodes = new LinkedHashSet<>(Arrays.asList("V001;C001", "V002"));

        @SuppressWarnings("unchecked")
        Set<String> actual = (Set<String>) ReflectionTestUtils.invokeMethod(
                service, "resolveCounterPartyCodes", sourceCodes);

        assertEquals(sourceCodes, actual);
    }
}
