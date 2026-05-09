package com.platform.accident.intelligence.client.factory;

import com.platform.accident.intelligence.client.AiModelProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiProviderFactoryTest {

    @Mock private AiModelProvider geminiProvider;
    @Mock private AiModelProvider openAiProvider;

    private AiProviderFactory factory;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(geminiProvider.getProviderName()).thenReturn("gemini");
        org.mockito.Mockito.lenient().when(openAiProvider.getProviderName()).thenReturn("gpt-4o");

        factory = new AiProviderFactory(List.of(geminiProvider, openAiProvider));
    }

    @Test
    @DisplayName("Factory Success: Should return correct provider when name matches (exact)")
    void getProvider_WhenExists_ReturnsProvider() {
        AiModelProvider result = factory.getProvider("gemini");

        assertThat(result).isEqualTo(geminiProvider);
        assertThat(result.getProviderName()).isEqualTo("gemini");
    }

    @Test
    @DisplayName("Factory Case Insensitivity: Should return provider regardless of upper/lower case")
    void getProvider_CaseInsensitivity_ReturnsProvider() {
        AiModelProvider result = factory.getProvider("GPT-4O");

        assertThat(result).isEqualTo(openAiProvider);
    }

    @Test
    @DisplayName("Factory Failure: Should throw RuntimeException when provider name is unknown")
    void getProvider_WhenNotFound_ThrowsException() {
        assertThatThrownBy(() -> factory.getProvider("deepseek"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Provider not found: deepseek");
    }

    @Test
    @DisplayName("Factory Boundary: Should handle empty provider names correctly")
    void getProvider_WithEmptyName_ThrowsException() {
        assertThatThrownBy(() -> factory.getProvider(""))
                .isInstanceOf(RuntimeException.class);
    }
}