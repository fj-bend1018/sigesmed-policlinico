import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ErrorMessageService {
  private readonly subject = new BehaviorSubject<string | null>(null);
  readonly message$ = this.subject.asObservable();
  publish(message: string): void { this.subject.next(message); }
  clear(): void { this.subject.next(null); }
}
