package de.tum.in.www1.artemis.web.rest;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.conflict;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.exam.ExerciseGroup;
import de.tum.in.www1.artemis.service.ExamAccessService;
import de.tum.in.www1.artemis.service.ExerciseGroupService;
import de.tum.in.www1.artemis.service.ExerciseService;
import de.tum.in.www1.artemis.service.UserService;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;

/**
 * REST controller for managing ExerciseGroup.
 */
@RestController
@RequestMapping("/api")
public class ExerciseGroupResource {

    private final Logger log = LoggerFactory.getLogger(ExerciseGroupResource.class);

    private static final String ENTITY_NAME = "exerciseGroup";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ExerciseGroupService exerciseGroupService;

    private final ExamAccessService examAccessService;

    private final UserService userService;

    private final ExerciseService exerciseService;

    private final AuditEventRepository auditEventRepository;

    public ExerciseGroupResource(ExerciseGroupService exerciseGroupService, ExamAccessService examAccessService, UserService userService, ExerciseService exerciseService,
            AuditEventRepository auditEventRepository) {
        this.exerciseGroupService = exerciseGroupService;
        this.examAccessService = examAccessService;
        this.userService = userService;
        this.exerciseService = exerciseService;
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * POST /courses/{courseId}/exams/{examId}/exerciseGroups : Create a new exercise group.
     *
     * @param courseId      the course to which the exercise group belongs to
     * @param examId        the exam to which the exercise group belongs to
     * @param exerciseGroup the exercise group to create
     * @return the ResponseEntity with status 201 (Created) and with the new exerciseGroup as body,
     *         or with status 400 (Bad Request) if the exerciseGroup has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/courses/{courseId}/exams/{examId}/exerciseGroups")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ExerciseGroup> createExerciseGroup(@PathVariable Long courseId, @PathVariable Long examId, @RequestBody ExerciseGroup exerciseGroup)
            throws URISyntaxException {
        log.debug("REST request to create an exercise group : {}", exerciseGroup);
        if (exerciseGroup.getId() != null) {
            throw new BadRequestAlertException("A new exerciseGroup cannot already have an ID", ENTITY_NAME, "idexists");
        }

        if (exerciseGroup.getExam() == null) {
            return conflict();
        }

        Optional<ResponseEntity<ExerciseGroup>> courseAndExamAccessFailure = examAccessService.checkCourseAndExamAccess(courseId, examId);
        if (courseAndExamAccessFailure.isPresent()) {
            return courseAndExamAccessFailure.get();
        }

        ExerciseGroup savedExerciseGroup = exerciseGroupService.save(exerciseGroup);
        return ResponseEntity.created(new URI("/api/courses/" + courseId + "/exams/" + examId + "/exerciseGroups/" + savedExerciseGroup.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, savedExerciseGroup.getTitle())).body(savedExerciseGroup);
    }

    /**
     * PUT /courses/{courseId}/exams/{examId}/exerciseGroups : Update an existing exercise group.
     *
     * @param courseId              the course to which the exercise group belongs to
     * @param examId                the exam to which the exercise group belongs to
     * @param updatedExerciseGroup  the exercise group to update
     * @return the ResponseEntity with status 200 (OK) and with the body of the updated exercise group
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/courses/{courseId}/exams/{examId}/exerciseGroups")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ExerciseGroup> updateExerciseGroup(@PathVariable Long courseId, @PathVariable Long examId, @RequestBody ExerciseGroup updatedExerciseGroup)
            throws URISyntaxException {
        log.debug("REST request to update an exercise group : {}", updatedExerciseGroup);
        if (updatedExerciseGroup.getId() == null) {
            return createExerciseGroup(courseId, examId, updatedExerciseGroup);
        }

        if (updatedExerciseGroup.getExam() == null) {
            return conflict();
        }

        Optional<ResponseEntity<ExerciseGroup>> accessFailure = examAccessService.checkCourseAndExamAndExerciseGroupAccess(courseId, examId, updatedExerciseGroup.getId());
        if (accessFailure.isPresent()) {
            return accessFailure.get();
        }

        ExerciseGroup result = exerciseGroupService.save(updatedExerciseGroup);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getTitle())).body(result);
    }

    /**
     * GET /courses/{courseId}/exams/{examId}/exerciseGroups/{exerciseGroupId} : Find an exercise group by id.
     *
     * @param courseId          the course to which the exercise group belongs to
     * @param examId            the exam to which the exercise group belongs to
     * @param exerciseGroupId   the id of the exercise group to find
     * @return the ResponseEntity with status 200 (OK) and with the found exercise group as body
     */
    @GetMapping("/courses/{courseId}/exams/{examId}/exerciseGroups/{exerciseGroupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ExerciseGroup> getExerciseGroup(@PathVariable Long courseId, @PathVariable Long examId, @PathVariable Long exerciseGroupId) {
        log.debug("REST request to get exercise group : {}", exerciseGroupId);
        Optional<ResponseEntity<ExerciseGroup>> accessFailure = examAccessService.checkCourseAndExamAndExerciseGroupAccess(courseId, examId, exerciseGroupId);
        return accessFailure.orElseGet(() -> ResponseEntity.ok(exerciseGroupService.findOneWithExam(exerciseGroupId)));
    }

    /**
     * GET courses/{courseId}/exams/{examId}/exerciseGroups : Get all exercise groups of the given exam
     *
     * @param courseId  the course to which the exercise groups belong to
     * @param examId    the exam to which the exercise groups belong to
     * @return the ResponseEntity with status 200 (OK) and a list of exercise groups. The list can be empty
     */
    @GetMapping("courses/{courseId}/exams/{examId}/exerciseGroups")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<List<ExerciseGroup>> getExerciseGroupsForExam(@PathVariable Long courseId, @PathVariable Long examId) {
        log.debug("REST request to get all exercise groups for exam : {}", examId);
        Optional<ResponseEntity<List<ExerciseGroup>>> courseAndExamAccessFailure = examAccessService.checkCourseAndExamAccess(courseId, examId);
        return courseAndExamAccessFailure.orElseGet(() -> ResponseEntity.ok(exerciseGroupService.findAllWithExamAndExercises(examId)));
    }

    /**
     * DELETE /courses/{courseId}/exams/{examId}/exerciseGroups/{exerciseGroupId} : Delete the exercise group with the given id.
     *
     * @param courseId          the course to which the exercise group belongs to
     * @param examId            the exam to which the exercise group belongs to
     * @param exerciseGroupId   the id of the exercise group to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/courses/{courseId}/exams/{examId}/exerciseGroups/{exerciseGroupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> deleteExerciseGroup(@PathVariable Long courseId, @PathVariable Long examId, @PathVariable Long exerciseGroupId) {
        log.info("REST request to delete exercise group : {}", exerciseGroupId);
        Optional<ResponseEntity<Void>> accessFailure = examAccessService.checkCourseAndExamAndExerciseGroupAccess(courseId, examId, exerciseGroupId);
        if (accessFailure.isPresent()) {
            return accessFailure.get();
        }

        ExerciseGroup exerciseGroup = exerciseGroupService.findOneWithExercises(exerciseGroupId);

        User user = userService.getUser();
        AuditEvent auditEvent = new AuditEvent(user.getLogin(), Constants.DELETE_EXERCISE_GROUP, "exerciseGroup=" + exerciseGroup.getTitle());
        auditEventRepository.add(auditEvent);
        log.info("User " + user.getLogin() + " has requested to delete the exercise group {}", exerciseGroup.getTitle());

        for (Exercise exercise : exerciseGroup.getExercises()) {
            exerciseService.delete(exercise.getId(), false, false);
        }

        exerciseGroupService.delete(exerciseGroupId);

        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, exerciseGroup.getTitle())).build();
    }
}