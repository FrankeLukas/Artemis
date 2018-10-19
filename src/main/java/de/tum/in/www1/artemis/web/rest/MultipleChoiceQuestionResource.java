package de.tum.in.www1.artemis.web.rest;

import com.codahale.metrics.annotation.Timed;
import de.tum.in.www1.artemis.domain.MultipleChoiceQuestion;
import de.tum.in.www1.artemis.repository.MultipleChoiceQuestionRepository;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing MultipleChoiceQuestion.
 */
@RestController
@RequestMapping("/api")
public class MultipleChoiceQuestionResource {

    private final Logger log = LoggerFactory.getLogger(MultipleChoiceQuestionResource.class);

    private static final String ENTITY_NAME = "multipleChoiceQuestion";

    private MultipleChoiceQuestionRepository multipleChoiceQuestionRepository;

    public MultipleChoiceQuestionResource(MultipleChoiceQuestionRepository multipleChoiceQuestionRepository) {
        this.multipleChoiceQuestionRepository = multipleChoiceQuestionRepository;
    }

    /**
     * POST  /multiple-choice-questions : Create a new multipleChoiceQuestion.
     *
     * @param multipleChoiceQuestion the multipleChoiceQuestion to create
     * @return the ResponseEntity with status 201 (Created) and with body the new multipleChoiceQuestion, or with status 400 (Bad Request) if the multipleChoiceQuestion has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/multiple-choice-questions")
    @Timed
    public ResponseEntity<MultipleChoiceQuestion> createMultipleChoiceQuestion(@RequestBody MultipleChoiceQuestion multipleChoiceQuestion) throws URISyntaxException {
        log.debug("REST request to save MultipleChoiceQuestion : {}", multipleChoiceQuestion);
        if (multipleChoiceQuestion.getId() != null) {
            throw new BadRequestAlertException("A new multipleChoiceQuestion cannot already have an ID", ENTITY_NAME, "idexists");
        }
        MultipleChoiceQuestion result = multipleChoiceQuestionRepository.save(multipleChoiceQuestion);
        return ResponseEntity.created(new URI("/api/multiple-choice-questions/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /multiple-choice-questions : Updates an existing multipleChoiceQuestion.
     *
     * @param multipleChoiceQuestion the multipleChoiceQuestion to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated multipleChoiceQuestion,
     * or with status 400 (Bad Request) if the multipleChoiceQuestion is not valid,
     * or with status 500 (Internal Server Error) if the multipleChoiceQuestion couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/multiple-choice-questions")
    @Timed
    public ResponseEntity<MultipleChoiceQuestion> updateMultipleChoiceQuestion(@RequestBody MultipleChoiceQuestion multipleChoiceQuestion) throws URISyntaxException {
        log.debug("REST request to update MultipleChoiceQuestion : {}", multipleChoiceQuestion);
        if (multipleChoiceQuestion.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        MultipleChoiceQuestion result = multipleChoiceQuestionRepository.save(multipleChoiceQuestion);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, multipleChoiceQuestion.getId().toString()))
            .body(result);
    }

    /**
     * GET  /multiple-choice-questions : get all the multipleChoiceQuestions.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of multipleChoiceQuestions in body
     */
    @GetMapping("/multiple-choice-questions")
    @Timed
    public List<MultipleChoiceQuestion> getAllMultipleChoiceQuestions() {
        log.debug("REST request to get all MultipleChoiceQuestions");
        return multipleChoiceQuestionRepository.findAll();
    }

    /**
     * GET  /multiple-choice-questions/:id : get the "id" multipleChoiceQuestion.
     *
     * @param id the id of the multipleChoiceQuestion to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the multipleChoiceQuestion, or with status 404 (Not Found)
     */
    @GetMapping("/multiple-choice-questions/{id}")
    @Timed
    public ResponseEntity<MultipleChoiceQuestion> getMultipleChoiceQuestion(@PathVariable Long id) {
        log.debug("REST request to get MultipleChoiceQuestion : {}", id);
        Optional<MultipleChoiceQuestion> multipleChoiceQuestion = multipleChoiceQuestionRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(multipleChoiceQuestion);
    }

    /**
     * DELETE  /multiple-choice-questions/:id : delete the "id" multipleChoiceQuestion.
     *
     * @param id the id of the multipleChoiceQuestion to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/multiple-choice-questions/{id}")
    @Timed
    public ResponseEntity<Void> deleteMultipleChoiceQuestion(@PathVariable Long id) {
        log.debug("REST request to delete MultipleChoiceQuestion : {}", id);

        multipleChoiceQuestionRepository.deleteById(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
