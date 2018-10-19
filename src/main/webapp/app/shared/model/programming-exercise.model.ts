export interface IProgrammingExercise {
    id?: number;
    baseRepositoryUrl?: string;
    solutionRepositoryUrl?: string;
    baseBuildPlanId?: string;
    publishBuildPlanUrl?: boolean;
    allowOnlineEditor?: boolean;
}

export class ProgrammingExercise implements IProgrammingExercise {
    constructor(
        public id?: number,
        public baseRepositoryUrl?: string,
        public solutionRepositoryUrl?: string,
        public baseBuildPlanId?: string,
        public publishBuildPlanUrl?: boolean,
        public allowOnlineEditor?: boolean
    ) {
        this.publishBuildPlanUrl = this.publishBuildPlanUrl || false;
        this.allowOnlineEditor = this.allowOnlineEditor || false;
    }
}
