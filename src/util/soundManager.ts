import { TOUCH_CLICK_BASE64, SUCCESS_CHIME_BASE64 } from './soundAssets';

function base64ToArrayBuffer(dataUri: string): ArrayBuffer {
  const base64 = dataUri.includes(',') ? dataUri.split(',')[1] : dataUri;
  const binaryString = window.atob(base64);
  const len = binaryString.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes.buffer;
}

class SoundManager {
  private isEnabled = true;
  private audioCtx: AudioContext | null = null;
  private touchBuffer: AudioBuffer | null = null;
  private completeBuffer: AudioBuffer | null = null;
  private touchAudioElement: HTMLAudioElement | null = null;
  private completeAudioElement: HTMLAudioElement | null = null;
  private isDecoding = false;

  constructor() {
    if (typeof window !== 'undefined') {
      try {
        this.touchAudioElement = new Audio(TOUCH_CLICK_BASE64);
        this.touchAudioElement.preload = 'auto';
        this.completeAudioElement = new Audio(SUCCESS_CHIME_BASE64);
        this.completeAudioElement.preload = 'auto';
      } catch {
        // Fallback gracefully
      }

      const initAudio = () => {
        this.getAudioContext();
        this.decodeEmbeddedBuffers();
        window.removeEventListener('click', initAudio);
        window.removeEventListener('touchstart', initAudio);
      };

      window.addEventListener('click', initAudio, { passive: true });
      window.addEventListener('touchstart', initAudio, { passive: true });
    }
  }

  public setSoundEnabled(enabled: boolean) {
    this.isEnabled = enabled;
  }

  public getSoundEnabled(): boolean {
    return this.isEnabled;
  }

  private getAudioContext(): AudioContext | null {
    if (typeof window === 'undefined') return null;
    try {
      const AudioContextClass =
        window.AudioContext ||
        (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (!this.audioCtx && AudioContextClass) {
        this.audioCtx = new AudioContextClass();
      }
      if (this.audioCtx && this.audioCtx.state === 'suspended') {
        this.audioCtx.resume();
      }
      return this.audioCtx;
    } catch {
      return null;
    }
  }

  private async decodeEmbeddedBuffers() {
    if (this.isDecoding) return;
    this.isDecoding = true;
    try {
      const ctx = this.getAudioContext();
      if (!ctx) return;

      if (!this.touchBuffer) {
        const touchArrayBuffer = base64ToArrayBuffer(TOUCH_CLICK_BASE64);
        this.touchBuffer = await ctx.decodeAudioData(touchArrayBuffer);
      }
      if (!this.completeBuffer) {
        const completeArrayBuffer = base64ToArrayBuffer(SUCCESS_CHIME_BASE64);
        this.completeBuffer = await ctx.decodeAudioData(completeArrayBuffer);
      }
    } catch {
      // Decode error fallback handled by HTML5 Audio elements
    } finally {
      this.isDecoding = false;
    }
  }

  /**
   * Mobile UI Tap (Plays embedded Base64 touch_click.mp3 with 0ms latency)
   */
  public playTouchClick() {
    if (!this.isEnabled) return;
    try {
      const ctx = this.getAudioContext();
      if (ctx && this.touchBuffer) {
        const source = ctx.createBufferSource();
        const gainNode = ctx.createGain();
        gainNode.gain.value = 0.95;
        source.buffer = this.touchBuffer;
        source.connect(gainNode);
        gainNode.connect(ctx.destination);
        source.start(0);
        return;
      }

      // Fallback: embedded HTML5 Audio Base64 element
      if (this.touchAudioElement) {
        this.touchAudioElement.currentTime = 0;
        this.touchAudioElement.play().catch(() => {
          const freshAudio = new Audio(TOUCH_CLICK_BASE64);
          freshAudio.play().catch(() => {});
        });
      } else {
        const freshAudio = new Audio(TOUCH_CLICK_BASE64);
        freshAudio.play().catch(() => {});
      }
      this.decodeEmbeddedBuffers();
    } catch {
      // Ignore
    }
  }

  /**
   * Voucher Saved Chime (Plays embedded Base64 success_chime.mp3 with 0ms latency)
   */
  public playComplete() {
    if (!this.isEnabled) return;
    try {
      const ctx = this.getAudioContext();
      if (ctx && this.completeBuffer) {
        const source = ctx.createBufferSource();
        const gainNode = ctx.createGain();
        gainNode.gain.value = 1.0;
        source.buffer = this.completeBuffer;
        source.connect(gainNode);
        gainNode.connect(ctx.destination);
        source.start(0);
      } else if (this.completeAudioElement) {
        this.completeAudioElement.currentTime = 0;
        this.completeAudioElement.play().catch(() => {
          const freshAudio = new Audio(SUCCESS_CHIME_BASE64);
          freshAudio.play().catch(() => {});
        });
      } else {
        const freshAudio = new Audio(SUCCESS_CHIME_BASE64);
        freshAudio.play().catch(() => {});
      }

      if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
        navigator.vibrate(25);
      }
    } catch {
      // Ignore
    }
  }

  // Unified aliases ensuring every click trigger routes to playTouchClick
  public playClick() {
    this.playTouchClick();
  }

  public playTap() {
    this.playTouchClick();
  }

  public playEdit() {
    this.playTouchClick();
  }

  public playDelete() {
    this.playTouchClick();
  }

  public playSave() {
    this.playTouchClick();
  }

  public playSuccess() {
    this.playComplete();
  }

  public playChime() {
    this.playComplete();
  }
}

export const soundManager = new SoundManager();
export const AppSoundManager2D = soundManager;
export const CleanAudio2D = soundManager;
