package de.tum.in.www1.artemis.web.rest;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.forbidden;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.exam.Exam;
import de.tum.in.www1.artemis.repository.ExamRepository;
import de.tum.in.www1.artemis.service.AuthorizationCheckService;
import de.tum.in.www1.artemis.service.CourseService;
import de.tum.in.www1.artemis.service.ExamService;
import de.tum.in.www1.artemis.service.UserService;
import de.tum.in.www1.artemis.web.rest.dto.mapper.ExamMapper;
import de.tum.in.www1.artemis.web.rest.dto.request.ExamRequestDTO;
import de.tum.in.www1.artemis.web.rest.dto.response.ExamResponseDTO;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;

/**
 * REST controller for managing Exam.
 */
@RestController
@RequestMapping("/api")
public class ExamResource {

    private final Logger log = LoggerFactory.getLogger(ExamResource.class);

    private static final String ENTITY_NAME = "exam";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final UserService userService;

    private final CourseService courseService;

    private final ExamService examService;

    private final ExamRepository examRepository;

    private final AuthorizationCheckService authCheckService;

    private final ExamMapper examMapper;

    public ExamResource(UserService userService, CourseService courseService, ExamService examService, ExamRepository examRepository, AuthorizationCheckService authCheckService,
            ExamMapper examMapper) {
        this.userService = userService;
        this.courseService = courseService;
        this.examService = examService;
        this.examRepository = examRepository;
        this.authCheckService = authCheckService;
        this.examMapper = examMapper;

    }

    /**
     * POST /courses/{courseId}/exams : Create a new exam.
     *
     * @param courseId the course to which the exam belongs
     * @param exam     the exam to create
     * @return the ResponseEntity with status 201 (Created) and with body the new exam, or with status 400 (Bad Request) if the exam has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/courses/{courseId}/exams")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ExamResponseDTO> createExam(@PathVariable Long courseId, @Valid @RequestBody ExamRequestDTO examRequestDTO) throws URISyntaxException {
        log.debug("REST request to create an exam : {}", examRequestDTO);
        if (examRequestDTO.id != null) {
            throw new BadRequestAlertException("A new exam cannot already have an ID", ENTITY_NAME, "idexists");
        }

        User user = userService.getUserWithGroupsAndAuthorities();
        Course course = courseService.findOne(courseId);
        if (!authCheckService.isAtLeastInstructorInCourse(course, user)) {
            return forbidden();
        }
        Exam exam = examMapper.examRequestDtoToExam(examRequestDTO);
        course.addExam(exam);
        Exam result = examService.save(exam);

        ExamResponseDTO examResponseDTO = examMapper.examToExamResponseDto(exam);

        return ResponseEntity.created(new URI("/api/courses/" + courseId + "/exams/" + examResponseDTO.id))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getTitle())).body(examResponseDTO);
    }

    @GetMapping("/courses/{courseId}/exams/{examId}")
    @PreAuthorize("hasAnyRole('USER','TA','INSTRUCTOR','ADMIN')")
    public ResponseEntity<ExamResponseDTO> getExam(@PathVariable Long examId, @PathVariable Long courseId) {
        User user = userService.getUserWithGroupsAndAuthorities();
        Course course = courseService.findOne(courseId);
        if (!authCheckService.isAtLeastStudentInCourse(course, user)) {
            return forbidden();
        }

        Optional<Exam> exam = examRepository.findById(examId);
        if (exam.isPresent()) {
            return ResponseEntity.ok().body(examMapper.examToExamResponseDto(exam.get()));
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PUT /courses/{courseId}/exams : Updates an existing exam.
     *
     * @param courseId    the course to which the exam belongs
     * @param updatedExam the exam to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated exam
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/courses/{courseId}/exams")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<ExamResponseDTO> updateExam(@PathVariable Long courseId, @RequestBody ExamRequestDTO updatedExam) throws URISyntaxException {
        log.debug("REST request to update an exam : {}", updatedExam);
        if (updatedExam.id == null) {
            return createExam(courseId, updatedExam);
        }

        User user = userService.getUserWithGroupsAndAuthorities();
        Course course = courseService.findOne(courseId);
        if (!authCheckService.isAtLeastInstructorInCourse(course, user)) {
            return forbidden();
        }

        Optional<Exam> existingExam = examRepository.findById(updatedExam.id);
        if (existingExam.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Exam result = examService.save(examMapper.examRequestDtoToExam(updatedExam));
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getTitle()))
                .body(examMapper.examToExamResponseDto(result));
    }
}
