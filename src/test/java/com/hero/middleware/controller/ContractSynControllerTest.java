package com.hero.middleware.controller;

import com.hero.middleware.dto.DeleteDraftContractsResultDTO;
import com.hero.middleware.dto.HistoryContractSyncDTO;
import com.hero.middleware.dto.HistoryContractSyncResultDTO;
import com.hero.middleware.service.ZhiShuSynService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContractSynController.class)
class ContractSynControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ZhiShuSynService zhiShuSynService;

    @Test
    void deleteDraftContractsCallsService() throws Exception {
        DeleteDraftContractsResultDTO result = new DeleteDraftContractsResultDTO();
        result.setTotalCount(1);
        when(zhiShuSynService.deleteAllDraftContracts()).thenReturn(result);

        mockMvc.perform(post("/api/contract/syn/draft-contracts/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(1));

        verify(zhiShuSynService).deleteAllDraftContracts();
    }

    @Test
    void multiThreadHistorySyncAcceptsUploadedFiles() throws Exception {
        HistoryContractSyncResultDTO result = new HistoryContractSyncResultDTO();
        result.addSuccess("H-TEST-001");
        when(zhiShuSynService.syncHistoryContractsMultiThread(any(HistoryContractSyncDTO.class)))
                .thenAnswer(invocation -> {
                    HistoryContractSyncDTO request = invocation.getArgument(0);
                    assertEquals("excel-content", new String(
                            Files.readAllBytes(Paths.get(request.getResolvedFilePath())), StandardCharsets.UTF_8));
                    assertEquals(Arrays.asList("H-TEST-001"), request.getContractNumbers());
                    assertTrue(Files.exists(Paths.get(request.getContractFileFallbackRoot())
                            .resolve("H-TEST-001").resolve("contract.pdf")));
                    assertEquals(3, request.getThreadCount());
                    assertEquals(20, request.getBatchSize());
                    return result;
                });

        MockMultipartFile contractFile = new MockMultipartFile(
                "contractFile", "contract-files.zip", "application/zip", buildContractFilesZip());
        MockMultipartFile excelFile = new MockMultipartFile(
                "excelFile", "history.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "excel-content".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/contract/syn/history/multi-thread")
                        .file(contractFile)
                        .file(excelFile)
                        .param("contractNumbers", "H-TEST-001")
                        .param("threadCount", "3")
                        .param("batchSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.successCount").value(1));

        verify(zhiShuSynService).syncHistoryContractsMultiThread(any(HistoryContractSyncDTO.class));
    }

    private byte[] buildContractFilesZip() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            zipOutputStream.putNextEntry(new ZipEntry("H-TEST-001/contract.pdf"));
            zipOutputStream.write("contract-content".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return outputStream.toByteArray();
    }
}
