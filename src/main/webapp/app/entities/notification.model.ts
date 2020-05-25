import { Moment } from 'moment';
import { BaseEntity } from 'app/shared/model/base-entity';
import { User } from 'app/core/user/user.model';

export enum NotificationType {
    SYSTEM = 'system',
    CONNECTION = 'connection',
    GROUP = 'group',
    SINGLE = 'single',
}

export class Notification implements BaseEntity {
    public id: number;
    public notificationType: NotificationType;
    public title: string;
    public text: string;
    public notificationDate: Moment | null;
    public target: string;
    public author: User;
}
