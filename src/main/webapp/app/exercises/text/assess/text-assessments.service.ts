import { Injectable } from '@angular/core';
import { SERVER_API_URL } from 'app/app.constants';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { Result } from 'app/entities/result.model';
import * as moment from 'moment';
import { ComplaintResponse } from 'app/entities/complaint-response.model';
import { Feedback } from 'app/entities/feedback.model';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { TextBlock } from 'app/entities/text-block.model';
import { TextBlockRef } from 'app/entities/text-block-ref.model';
import { cloneDeep } from 'lodash';
import { TextSubmission } from 'app/entities/text-submission.model';

type EntityResponseType = HttpResponse<Result>;
type TextAssessmentDTO = { feedbacks: Feedback[]; textBlocks: TextBlock[] };

@Injectable({
    providedIn: 'root',
})
export class TextAssessmentsService {
    private readonly resourceUrl = SERVER_API_URL + 'api/text-assessments';

    constructor(private http: HttpClient) {}

    /**
     * Saves the passed feedback items of the assessment.
     * @param exerciseId id of the exercise the assessed submission was made to of type {number}
     * @param resultId id of the corresponding result of type {number}
     * @param feedbacks list of feedback made during assessment of type {Feedback[]}
     * @param textBlocks list of text blocks of type {TextBlock[]}
     */
    public save(exerciseId: number, resultId: number, feedbacks: Feedback[], textBlocks: TextBlock[]): Observable<EntityResponseType> {
        const body = TextAssessmentsService.prepareFeedbacksAndTextblocksForRequest(feedbacks, textBlocks);
        return this.http
            .put<Result>(`${this.resourceUrl}/exercise/${exerciseId}/result/${resultId}`, body, { observe: 'response' })
            .map((res: EntityResponseType) => TextAssessmentsService.convertResponse(res));
    }

    /**
     * Submits the passed feedback items of the assessment.
     * @param exerciseId id of the exercise the assessed submission was made to of type {number}
     * @param resultId id of the corresponding result of type {number}
     * @param feedbacks list of feedback made during assessment of type {Feedback[]}
     * @param textBlocks list of text blocks of type {TextBlock[]}
     */
    public submit(exerciseId: number, resultId: number, feedbacks: Feedback[], textBlocks: TextBlock[]): Observable<EntityResponseType> {
        const body = TextAssessmentsService.prepareFeedbacksAndTextblocksForRequest(feedbacks, textBlocks);
        return this.http
            .put<Result>(`${this.resourceUrl}/exercise/${exerciseId}/result/${resultId}/submit`, body, { observe: 'response' })
            .map((res: EntityResponseType) => TextAssessmentsService.convertResponse(res));
    }

    /**
     * Updates an assessment after a complaint.
     * @param feedbacks list of feedback made during assessment of type {Feedback[]}
     * @param textBlocks list of text blocks of type {TextBlock[]}
     * @param complaintResponse response on the complaint of type {ComplaintResponse}
     * @param submissionId id of corresponding submission of type {number}
     */
    public updateAssessmentAfterComplaint(
        feedbacks: Feedback[],
        textBlocks: TextBlock[],
        complaintResponse: ComplaintResponse,
        submissionId: number,
    ): Observable<EntityResponseType> {
        const url = `${this.resourceUrl}/text-submissions/${submissionId}/assessment-after-complaint`;
        const assessmentUpdate = {
            feedbacks,
            textBlocks,
            complaintResponse,
        };
        return this.http
            .put<Result>(url, assessmentUpdate, { observe: 'response' })
            .map((res: EntityResponseType) => TextAssessmentsService.convertResponse(res));
    }

    /**
     * Cancels an assessment.
     * @param exerciseId id of the exercise the assessed submission was made to of type {number}
     * @param submissionId id of corresponding submission of type {number}
     */
    public cancelAssessment(exerciseId: number, submissionId: number): Observable<void> {
        return this.http.put<void>(`${this.resourceUrl}/exercise/${exerciseId}/submission/${submissionId}/cancel-assessment`, null);
    }

    /**
     * Get all feedback items for a submission.
     * @param submissionId id of the submission for which the feedback items should be retrieved of type {number}
     */
    public getFeedbackDataForExerciseSubmission(submissionId: number): Observable<StudentParticipation> {
        return this.http
            .get<StudentParticipation>(`${this.resourceUrl}/submission/${submissionId}`, { observe: 'response' })
            .pipe(
                // Wire up Result and Submission
                tap((response) => (response.body!.submissions[0].result = response.body!.results[0])),
                tap((response) => (response.body!.submissions[0].participation = response.body!)),
                tap((response) => (response.body!.results[0].submission = response.body!.submissions[0])),
                tap((response) => (response.body!.results[0].participation = response.body!)),
                // Make sure Feedbacks Array is initialized
                tap((response) => (response.body!.results[0].feedbacks = response.body!.results[0].feedbacks || [])),

                // Add the jwt token for tutor assessment tracking if athene profile is active, otherwise set it null
                tap((response) => ((response.body!.submissions[0] as TextSubmission).atheneTextAssessmentTrackingToken = response.headers.get('x-athene-tracking-authorization'))),
                map((response) => response.body!),
            );
    }

    /**
     * Gets an example result for defined exercise and submission.
     * @param exerciseId id of the exercise for which the example result should be retrieved of type {number}
     * @param submissionId id of the submission for which the example result should be retrieved of type {number}
     */
    public getExampleResult(exerciseId: number, submissionId: number): Observable<Result> {
        return this.http.get<Result>(`${this.resourceUrl}/exercise/${exerciseId}/submission/${submissionId}/example-result`);
    }

    private static prepareFeedbacksAndTextblocksForRequest(feedbacks: Feedback[], textBlocks: TextBlock[]): TextAssessmentDTO {
        feedbacks = feedbacks.map((f) => {
            f = Object.assign({}, f);
            f.result = null;
            return f;
        });
        textBlocks = textBlocks.map((tb) => {
            tb = Object.assign({}, tb);
            tb.submission = undefined;
            return tb;
        });

        return { feedbacks, textBlocks };
    }

    private static convertResponse(res: EntityResponseType): EntityResponseType {
        const result = TextAssessmentsService.convertItemFromServer(res.body!);

        if (result.completionDate) {
            result.completionDate = moment(result.completionDate);
        }
        if (result.submission && result.submission.submissionDate) {
            result.submission.submissionDate = moment(result.submission.submissionDate);
        }
        if (result.participation && result.participation.initializationDate) {
            result.participation.initializationDate = moment(result.participation.initializationDate);
        }

        return res.clone({ body: result });
    }

    private static convertItemFromServer(result: Result): Result {
        return Object.assign({}, result);
    }

    /**
     * Match given text blocks and feedback items by text block references.
     * @param blocks list of text blocks of type {TextBlock[]}
     * @param feedbacks list of feedback made during assessment of type {Feedback[]}
     */
    public static matchBlocksWithFeedbacks(blocks: TextBlock[], feedbacks: Feedback[]): TextBlockRef[] {
        return blocks.map(
            (block: TextBlock) =>
                new TextBlockRef(
                    block,
                    feedbacks.find(({ reference }) => block.id === reference),
                ),
        );
    }

    /**
     * Track the change of the Feedback in Athene.
     *
     * The routing to athene is done using nginx on the production server.
     *
     * @param submission - The submission object that holds the data that is tracked
     * @param origin - The method that calls the the tracking method
     */
    public trackAssessment(submission: TextSubmission | null, origin: string) {
        if (submission?.atheneTextAssessmentTrackingToken) {
            // clone submission and resolve circular json properties
            const submissionForSending = cloneDeep(submission);
            delete submissionForSending.participation?.submissions;
            delete submissionForSending.participation?.exercise?.course;
            delete submissionForSending.participation?.exercise?.exerciseGroup;
            delete submissionForSending.atheneTextAssessmentTrackingToken;

            // eslint-disable-next-line chai-friendly/no-unused-expressions
            submissionForSending.participation?.results?.forEach((result) => {
                delete result.participation;
                delete result.submission;
            });

            const trackingObject = {
                origin,
                textBlocks: submissionForSending.blocks,
                participation: submissionForSending.participation,
            };

            // The request is directly routed to athene via nginx
            this.http
                .post(`${SERVER_API_URL}/athene-tracking/text-exercise-assessment`, trackingObject, {
                    headers: { 'X-Athene-Tracking-Authorization': submission.atheneTextAssessmentTrackingToken },
                })
                .subscribe();
        }
    }
}
