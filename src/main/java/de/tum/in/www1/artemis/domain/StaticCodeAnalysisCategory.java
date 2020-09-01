package de.tum.in.www1.artemis.domain;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.tum.in.www1.artemis.domain.enumeration.CategoryState;

/**
 * Entity for storing static code analysis categories and their settings.
 */
@Entity
@Table(name = "static_code_analysis_category")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class StaticCodeAnalysisCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "penalty")
    private Integer penalty;

    @Column(name = "max_penalty")
    private Integer maxPenalty;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private CategoryState state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties("staticCodeAnalysisCategories")
    private ProgrammingExercise exercise;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPenalty() {
        return penalty;
    }

    public void setPenalty(Integer penalty) {
        this.penalty = penalty;
    }

    public Integer getMaxPenalty() {
        return maxPenalty;
    }

    public void setMaxPenalty(Integer maxPenalty) {
        this.maxPenalty = maxPenalty;
    }

    public CategoryState getState() {
        return state;
    }

    public void setState(CategoryState state) {
        this.state = state;
    }

    public ProgrammingExercise getExercise() {
        return exercise;
    }

    public void setProgrammingExercise(ProgrammingExercise exercise) {
        this.exercise = exercise;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StaticCodeAnalysisCategory that = (StaticCodeAnalysisCategory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
