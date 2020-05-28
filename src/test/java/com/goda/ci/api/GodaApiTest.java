package com.goda.ci.api;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class GodaApiTest {

    @Value("${env.name:defaultGoda}")
    private String nameFromEnv;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

   @Test
    void getHelloGodaApi() throws Exception {
       mockMvc.perform(MockMvcRequestBuilders.get("/").accept(MediaType.APPLICATION_JSON)).andExpect(status()
               .isOk()).andExpect(content().string(equalTo("{\"msg\":\"Hello all (Another change 2.0.0)! from Goda!\",\"anotherProp\":\""+nameFromEnv+"\",\"envProp\":\""+nameFromEnv+"\"}")));
    }
}