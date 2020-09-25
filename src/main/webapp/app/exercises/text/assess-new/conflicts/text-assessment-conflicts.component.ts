import { Component, OnInit, AfterViewInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { JhiAlertService } from 'ng-jhipster';
import { TranslateService } from '@ngx-translate/core';

import { TextAssessmentBaseComponent } from 'app/exercises/text/assess-new/text-assessment-base.component';
import { TextSubmission } from 'app/entities/text-submission.model';
import { TextAssessmentsService } from 'app/exercises/text/assess/text-assessments.service';
import { TextBlockRef } from 'app/entities/text-block-ref.model';
import { TextBlock, TextBlockType } from 'app/entities/text-block.model';
import { TextExercise } from 'app/entities/text-exercise.model';
import { Result } from 'app/entities/result.model';
import { TextAssessmentConflict } from 'app/entities/text-assessment-conflict';
import { AccountService } from 'app/core/auth/account.service';
import { StructuredGradingCriterionService } from 'app/exercises/shared/structured-grading-criterion/structured-grading-criterion.service';

import interact from 'interactjs';
import * as moment from 'moment';

@Component({
    selector: 'jhi-text-assessment-conflicts',
    templateUrl: './text-assessment-conflicts.component.html',
    styleUrls: ['./text-assessment-conflicts.component.scss'],
})
export class TextAssessmentConflictsComponent extends TextAssessmentBaseComponent implements OnInit, AfterViewInit {
    conflictingSubmissions: TextSubmission[] | null;
    leftSubmission: TextSubmission | null;
    leftTextBlockRefs: TextBlockRef[];
    leftUnusedTextBlockRefs: TextBlockRef[];
    leftTotalScore: number;
    leftFeedbackId: number;
    rightSubmission: TextSubmission | null;
    rightTextBlockRefs: TextBlockRef[];
    rightUnusedTextBlockRefs: TextBlockRef[];
    rightTotalScore: number;
    conflictingAssessments: TextAssessmentConflict[];
    overrideBusy = false;
    markBusy = false;
    isOverrideDisabled = true;
    isMarkingDisabled = true;
    selectedRightFeedbackId: number | null;

    private get textBlocksWithFeedbackForLeftSubmission(): TextBlock[] {
        return [...this.leftTextBlockRefs, ...this.leftUnusedTextBlockRefs]
            .filter(({ block, feedback }) => block.type === TextBlockType.AUTOMATIC || !!feedback)
            .map(({ block }) => block);
    }

    constructor(
        private activatedRoute: ActivatedRoute,
        private router: Router,
        protected accountService: AccountService,
        protected assessmentsService: TextAssessmentsService,
        protected jhiAlertService: JhiAlertService,
        translateService: TranslateService,
        protected structuredGradingCriterionService: StructuredGradingCriterionService,
    ) {
        super(jhiAlertService, accountService, assessmentsService, translateService, structuredGradingCriterionService);
        const state = router.getCurrentNavigation()?.extras.state as { submission: TextSubmission };
        this.leftFeedbackId = Number(activatedRoute.snapshot.paramMap.get('feedbackId'));
        this.leftSubmission = state.submission;
        this.exercise = this.leftSubmission.participation?.exercise as TextExercise;
        this.leftTextBlockRefs = [];
        this.leftUnusedTextBlockRefs = [];
        this.rightTextBlockRefs = [];
        this.rightUnusedTextBlockRefs = [];
        this.conflictingAssessments = [];
    }

    ngAfterViewInit() {
        interact('.movable')
            .resizable({
                edges: { left: '.draggable-left', right: false, bottom: false, top: false },
                modifiers: [
                    // Set maximum width
                    interact.modifiers!.restrictSize({
                        min: { width: 500, height: 0 },
                        max: { width: 750, height: 2000 },
                    }),
                ],
                inertia: true,
            })
            .on('resizestart', (event: any) => {
                event.target.classList.add('card-resizable');
            })
            .on('resizeend', (event: any) => {
                event.target.classList.remove('card-resizable');
            })
            .on('resizemove', (event: any) => {
                const target = event.target;
                target.style.width = event.rect.width + 'px';
            });
    }

    async ngOnInit() {
        await super.ngOnInit();
        this.activatedRoute.data.subscribe(({ conflictingTextSubmissions }) => this.setPropertiesFromServerResponse(conflictingTextSubmissions));
    }

    didChangeConflictIndex(conflictIndex: number) {
        this.rightUnusedTextBlockRefs = [];
        this.rightTextBlockRefs = [];
        this.conflictingAssessments = [];
        this.selectedRightFeedbackId = null;
        this.isMarkingDisabled = true;
        this.setConflictingSubmission(conflictIndex - 1);
    }

    private setPropertiesFromServerResponse(conflictingTextSubmissions: TextSubmission[]) {
        this.conflictingSubmissions = conflictingTextSubmissions;
        this.prepareTextBlocksAndFeedbackFor(this.leftSubmission!, this.leftTextBlockRefs, this.leftUnusedTextBlockRefs);
        this.leftTotalScore = this.computeTotalScore(this.leftSubmission!.result.feedbacks);
        this.setConflictingSubmission(0);
    }

    private setConflictingSubmission(index: number) {
        this.rightSubmission = this.conflictingSubmissions ? this.conflictingSubmissions[index] : null;
        this.prepareTextBlocksAndFeedbackFor(this.rightSubmission!, this.rightTextBlockRefs, this.rightUnusedTextBlockRefs);
        this.rightTotalScore = this.computeTotalScore(this.rightSubmission!.result.feedbacks);
        this.conflictingAssessments = this.leftSubmission?.result.feedbacks.find((f) => f.id === this.leftFeedbackId)?.conflictingTextAssessments || [];
    }

    isAssessor(result: Result): boolean {
        return result !== null && result.assessor && result.assessor.id === this.userId;
    }

    canOverride(result: Result): boolean {
        if (this.exercise) {
            if (this.isAtLeastInstructor) {
                // Instructors can override any assessment at any time.
                return true;
            }
            let isBeforeAssessmentDueDate = true;
            // Add check as the assessmentDueDate must not be set for exercises
            if (this.exercise.assessmentDueDate) {
                isBeforeAssessmentDueDate = moment().isBefore(this.exercise.assessmentDueDate!);
            }
            // tutors are allowed to override one of their assessments before the assessment due date.
            return this.isAssessor(result) && isBeforeAssessmentDueDate;
        }
        return false;
    }

    overrideLeftSubmission() {
        if (!this.leftSubmission || !this.leftSubmission.result || !this.leftSubmission.result.id || this.overrideBusy) {
            return;
        }

        this.overrideBusy = true;
        this.assessmentsService
            .submit(this.exercise!.id, this.leftSubmission!.result.id, this.leftSubmission!.result.feedbacks, this.textBlocksWithFeedbackForLeftSubmission)
            .subscribe(
                (response) => this.handleSaveOrSubmitSuccessWithAlert(response, 'artemisApp.textAssessment.submitSuccessful'),
                (error: HttpErrorResponse) => this.handleError(error),
            );
    }

    leftTextBlockRefsChange(): void {
        this.leftTotalScore = this.computeTotalScore(this.leftSubmission!.result.feedbacks);
        this.isOverrideDisabled = false;
    }

    didSelectConflictingFeedback(rightFeedbackId: number): void {
        this.selectedRightFeedbackId = rightFeedbackId !== this.selectedRightFeedbackId ? rightFeedbackId : null;
        this.isMarkingDisabled = !this.selectedRightFeedbackId;
    }

    markSelectedAsNoConflict(): void {
        if (this.markBusy || !this.selectedRightFeedbackId) {
            return;
        }

        const textAssessmentConflictId = this.conflictingAssessments.find((conflictingAssessment) => conflictingAssessment.conflictingFeedbackId === this.selectedRightFeedbackId)
            ?.id;

        if (!textAssessmentConflictId) {
            return;
        }

        this.markBusy = true;
        this.assessmentsService.setConflictsAsSolved(textAssessmentConflictId).subscribe(
            (response) => this.handleSolveConflictsSuccessWithAlert(response, ''),
            (error) => this.handleSolveConflictsError(error),
        );
    }

    private prepareTextBlocksAndFeedbackFor(submission: TextSubmission, textBlockRefs: TextBlockRef[], unusedTextBlockRefs: TextBlockRef[]): void {
        const feedbackList = submission.result.feedbacks || [];
        const matchBlocksWithFeedbacks = TextAssessmentsService.matchBlocksWithFeedbacks(submission?.blocks || [], feedbackList);
        this.sortAndSetTextBlockRefs(matchBlocksWithFeedbacks, textBlockRefs, unusedTextBlockRefs, submission);
    }

    protected handleSaveOrSubmitSuccessWithAlert(response: HttpResponse<Result>, translationKey: string): void {
        super.handleSaveOrSubmitSuccessWithAlert(response, translationKey);
        this.overrideBusy = false;
        this.isOverrideDisabled = true;
    }

    private handleSolveConflictsSuccessWithAlert(response: TextAssessmentConflict, translationKey: string): void {
        this.jhiAlertService.success(translationKey);
        this.markBusy = false;
        this.isMarkingDisabled = true;
    }

    protected handleError(error: HttpErrorResponse): void {
        super.handleError(error);
        this.overrideBusy = false;
        this.isOverrideDisabled = true;
    }

    private handleSolveConflictsError(error: HttpErrorResponse): void {
        super.handleError(error);
        this.markBusy = false;
        this.isMarkingDisabled = true;
    }
}
