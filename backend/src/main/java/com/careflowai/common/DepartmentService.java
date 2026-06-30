package com.careflowai.common;

import com.careflowai.queue.QueueEntryRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {

    private static final List<String> DEFAULT_DEPARTMENTS = List.of(
        "Emergency",
        "Pediatrics",
        "Orthopedics",
        "General",
        "Cardiology",
        "Neurology"
    );

    private final QueueEntryRepository queueEntryRepository;

    public DepartmentService(QueueEntryRepository queueEntryRepository) {
        this.queueEntryRepository = queueEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<String> departments() {
        Set<String> departments = new LinkedHashSet<>(DEFAULT_DEPARTMENTS);
        queueEntryRepository.findDistinctDepartments().stream()
            .filter(department -> department != null && !department.isBlank())
            .forEach(departments::add);
        return List.copyOf(departments);
    }
}
