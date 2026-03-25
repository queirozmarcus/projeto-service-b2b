export interface AuthMessage {
  type: 'logout' | 'login';
}

export class AuthBroadcaster {
  private channel: BroadcastChannel | null = null;

  constructor() {
    if (typeof window !== 'undefined') {
      try {
        this.channel = new BroadcastChannel('auth');
      } catch {
        console.warn('BroadcastChannel não suportado neste browser');
      }
    }
  }

  broadcast(message: AuthMessage): void {
    this.channel?.postMessage(message);
  }

  onMessage(callback: (message: AuthMessage) => void): void {
    if (this.channel) {
      this.channel.onmessage = (event: MessageEvent<AuthMessage>) =>
        callback(event.data);
    }
  }

  close(): void {
    this.channel?.close();
  }
}

export const authBroadcaster = new AuthBroadcaster();
