package ru.mirea.callcenter;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ApiContractTest {
 @Autowired MockMvc mvc;
 @Test void unauthenticatedCannotRead() throws Exception { mvc.perform(get("/api/appeals")).andExpect(status().isUnauthorized()).andExpect(jsonPath("status").value(401)); }
 @Test @WithMockUser(roles="VIEWER") void readerCannotWrite() throws Exception {
 mvc.perform(delete("/api/appeals/00000000-0000-0000-0000-000000000001")).andExpect(status().isForbidden());
 mvc.perform(get("/api/admin/audit")).andExpect(status().isForbidden()); }
 @Test @WithMockUser(roles="EDITOR") void invalidDtoReturns400() throws Exception {
 mvc.perform(post("/api/appeals").contentType("application/json").content("{}"))
 .andExpect(status().isBadRequest()).andExpect(jsonPath("validationErrors.topic").exists()); }
 @Test @WithMockUser(roles="VIEWER") void paginationAndSortingAreBounded() throws Exception {
 mvc.perform(get("/api/appeals?size=101")).andExpect(status().isBadRequest());
 mvc.perform(get("/api/appeals?sort=passwordHash,asc")).andExpect(status().isBadRequest());
 mvc.perform(get("/api/appeals").param("q", "impact>101")).andExpect(status().isBadRequest());
 mvc.perform(get("/api/appeals?sort=status,asc")).andExpect(status().isOk()).andExpect(jsonPath("content").isArray()); }
 @Test @WithMockUser(roles="VIEWER") void missingRecord404() throws Exception {
 mvc.perform(get("/api/appeals/00000000-0000-0000-0000-000000000001")).andExpect(status().isNotFound()); }
 @Test void publicHealth() throws Exception { mvc.perform(get("/actuator/health")).andExpect(status().isOk()); }
}
