package com.platform.accident.intelligence.client.factory;

import com.platform.accident.intelligence.client.AiModelProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AiProviderFactory {

    private final List<AiModelProvider> providers;

    public AiModelProvider getProvider(String name) {
        return providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Provider not found: " + name));
    }
}

