package com.example.bugtracker.repo;

import com.example.bugtracker.model.Bug;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FileBugRepository {

    private final ObjectMapper mapper = new ObjectMapper();
    private final File dataFile = new File("data/bugs.json");
    private final AtomicLong idCounter = new AtomicLong(1);
    private List<Bug> cache = new ArrayList<>();

    public FileBugRepository() {
        mapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    private synchronized void init() {
        try {
            File dir = dataFile.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            if (!dataFile.exists()) {
                mapper.writeValue(dataFile, Collections.emptyList());
            }
            byte[] bytes = Files.readAllBytes(dataFile.toPath());
            cache = mapper.readValue(bytes, new TypeReference<List<Bug>>() {});
            long max = cache.stream().mapToLong(b -> b.getId() == null ? 0L : b.getId()).max().orElse(0L);
            idCounter.set(max + 1);
        } catch (Exception e) {
            e.printStackTrace();
            cache = new ArrayList<>();
        }
    }

    private synchronized void persist() {
        try {
            mapper.writeValue(dataFile, cache);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized List<Bug> findAll() {
        return new ArrayList<>(cache);
    }

    public synchronized Optional<Bug> findById(Long id) {
        return cache.stream().filter(b -> Objects.equals(b.getId(), id)).findFirst();
    }

    public synchronized Bug save(Bug bug) {
        if (bug.getId() == null) {
            bug.setId(idCounter.getAndIncrement());
            if (bug.getCreatedAt() == null) bug.setCreatedAt(java.time.LocalDateTime.now());
            if (bug.getStatus() == null) bug.setStatus("OPEN");
            if (bug.getPriority() == null) bug.setPriority("MEDIUM");
            cache.add(bug);
        } else {
            for (int i = 0; i < cache.size(); i++) {
                Bug b = cache.get(i);
                if (Objects.equals(b.getId(), bug.getId())) {
                    bug.setCreatedAt(b.getCreatedAt());
                    cache.set(i, bug);
                    break;
                }
            }
        }
        persist();
        return bug;
    }

    public synchronized boolean delete(Long id) {
        boolean removed = cache.removeIf(b -> Objects.equals(b.getId(), id));
        if (removed) persist();
        return removed;
    }
}
