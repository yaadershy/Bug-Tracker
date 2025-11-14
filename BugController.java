package com.example.bugtracker.controller;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.bugtracker.model.Bug;
import com.example.bugtracker.model.Comment;
import com.example.bugtracker.repo.FileBugRepository;

@RestController
@RequestMapping("/api/bugs")
public class BugController {

    private final FileBugRepository repo;
    private final AtomicLong commentIdGen = new AtomicLong(1);
    private final File uploadDir = new File("data/uploads");

    public BugController(FileBugRepository repo) {
        this.repo = repo;
        if (!uploadDir.exists())
            uploadDir.mkdirs();
    }

    // list with optional sorting & filter
    @GetMapping
    public List<Bug> listAll(@RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "desc") String order,
            @RequestParam(required = false) String status) {
        List<Bug> all = repo.findAll();
        if (status != null && !status.isBlank()) {
            all = all.stream().filter(b -> status.equalsIgnoreCase(b.getStatus())).collect(Collectors.toList());
        }

        Comparator<Bug> cmp = Comparator.comparing(Bug::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        if ("priority".equalsIgnoreCase(sort)) {
            Map<String, Integer> weight = Map.of("HIGH", 3, "MEDIUM", 2, "LOW", 1);
            cmp = Comparator.comparing(b -> weight.getOrDefault(b.getPriority(), 2));
        } else if ("createdAt".equalsIgnoreCase(sort)) {
            cmp = Comparator.comparing(Bug::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if ("asc".equalsIgnoreCase(order))
            Collections.sort(all, cmp);
        else
            Collections.sort(all, cmp.reversed());
        return all;
    }

    // Create: public
    @PostMapping
    public ResponseEntity<Bug> create(@RequestBody Bug bug) {
        if (bug.getCreatedAt() == null)
            bug.setCreatedAt(LocalDateTime.now());
        if (bug.getStatus() == null)
            bug.setStatus("OPEN");
        if (bug.getPriority() == null)
            bug.setPriority("MEDIUM");
        return ResponseEntity.ok(repo.save(bug));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bug> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Update: public
    @PutMapping("/{id}")
    public ResponseEntity<Bug> update(@PathVariable Long id, @RequestBody Bug update) {
        Optional<Bug> exOpt = repo.findById(id);
        if (exOpt.isEmpty())
            return ResponseEntity.notFound().build();
        Bug ex = exOpt.get();
        ex.setTitle(update.getTitle());
        ex.setDescription(update.getDescription());
        ex.setStatus(update.getStatus());
        ex.setPriority(update.getPriority());
        ex.setAssignedTo(update.getAssignedTo());
        repo.save(ex);
        return ResponseEntity.ok(ex);
    }

    // Delete: public
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (repo.delete(id))
            return ResponseEntity.noContent().build();
        else
            return ResponseEntity.notFound().build();
    }

    // Assign: public
    @PostMapping("/{id}/assign")
    public ResponseEntity<Bug> assign(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String assignee = body.get("assignedTo");
        Optional<Bug> b = repo.findById(id);
        if (b.isEmpty())
            return ResponseEntity.notFound().build();
        Bug bug = b.get();
        bug.setAssignedTo(assignee);
        repo.save(bug);
        return ResponseEntity.ok(bug);
    }

    // Add a comment. Accepts {"author":"name","text":"..."}; author optional ->
    // "Anonymous"
    @PostMapping("/{id}/comments")
    public ResponseEntity<Comment> addComment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank())
            return ResponseEntity.badRequest().build();
        String author = body.getOrDefault("author", "Anonymous");
        Optional<Bug> b = repo.findById(id);
        if (b.isEmpty())
            return ResponseEntity.notFound().build();
        Bug bug = b.get();
        long cid = commentIdGen.getAndIncrement();
        Comment c = new Comment(cid, author, text);
        List<Comment> comments = bug.getComments();
        if (comments == null)
            comments = new ArrayList<>();
        comments.add(c);
        bug.setComments(comments);
        repo.save(bug);
        return ResponseEntity.ok(c);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<Comment>> listComments(@PathVariable Long id) {
        Optional<Bug> b = repo.findById(id);
        if (b.isEmpty())
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(b.get().getComments());
    }

    // Upload attachments (public)
    @PostMapping("/{id}/attachments")
    public ResponseEntity<List<String>> upload(@PathVariable Long id, @RequestPart("files") MultipartFile[] files) {
        Optional<Bug> b = repo.findById(id);
        if (b.isEmpty())
            return ResponseEntity.notFound().build();
        Bug bug = b.get();

        List<String> saved = new ArrayList<>();
        for (MultipartFile mf : files) {
            try {
                String original = StringUtils.cleanPath(Objects.requireNonNull(mf.getOriginalFilename()));
                String name = System.currentTimeMillis() + "_" + original;
                File dest = new File(uploadDir, name);
                mf.transferTo(dest);
                saved.add("uploads/" + name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        List<String> at = bug.getAttachments();
        if (at == null)
            at = new ArrayList<>();
        at.addAll(saved);
        bug.setAttachments(at);
        repo.save(bug);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}/attachment")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long id, @RequestParam String name) {
        try {
            File f = new File("data/uploads", name);
            if (!f.exists())
                return ResponseEntity.notFound().build();
            byte[] content = Files.readAllBytes(f.toPath());
            return ResponseEntity.ok().body(content);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
