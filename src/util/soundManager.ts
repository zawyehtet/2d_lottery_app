class SoundManager {
  private isEnabled = true;
  private audioCtx: AudioContext | null = null;
  private touchBuffer: AudioBuffer | null = null;
  private completeBuffer: AudioBuffer | null = null;
  private isPreloading = false;

  constructor() {
    if (typeof window !== 'undefined') {
      const initAudio = () => {
        this.getAudioContext();
        this.loadAudioFiles();
        window.removeEventListener('click', initAudio);
        window.removeEventListener('touchstart', initAudio);
      };
      window.addEventListener('click', initAudio, { passive: true });
      window.addEventListener('touchstart', initAudio, { passive: true });
      // Start background fetch immediately
      this.loadAudioFiles();
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

  private async loadAudioFiles() {
    if (this.isPreloading) return;
    this.isPreloading = true;
    try {
      const ctx = this.getAudioContext();
      if (!ctx) return;

      if (!this.touchBuffer) {
        const res = await fetch('/sounds/touch_click.mp3');
        const arrayBuf = await res.arrayBuffer();
        this.touchBuffer = await ctx.decodeAudioData(arrayBuf);
      }
      if (!this.completeBuffer) {
        const res = await fetch('/sounds/success_chime.mp3');
        const arrayBuf = await res.arrayBuffer();
        this.completeBuffer = await ctx.decodeAudioData(arrayBuf);
      }
    } catch {
      // Ignore background load errors
    } finally {
      this.isPreloading = false;
    }
  }

  /**
   * Mobile UI Tap (Plays strictly provided res/raw/touch_click audio file)
   */
  public playTouchClick() {
    if (!this.isEnabled) return;
    try {
      const ctx = this.getAudioContext();
      if (ctx && this.touchBuffer) {
        const source = ctx.createBufferSource();
        const gainNode = ctx.createGain();
        gainNode.gain.value = 0.9;
        source.buffer = this.touchBuffer;
        source.connect(gainNode);
        gainNode.connect(ctx.destination);
        source.start(0);
        return;
      }

      // Fallback: HTML5 Audio playing the exact file directly
      const audio = new Audio('/sounds/touch_click.mp3');
      audio.volume = 0.9;
      audio.play().catch(() => {});
      this.loadAudioFiles();
    } catch {
      // Ignore
    }
  }

  /**
   * Voucher Saved Chime (Plays strictly provided res/raw/success_chime audio file)
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
      } else {
        const audio = new Audio('/sounds/success_chime.mp3');
        audio.volume = 1.0;
        audio.play().catch(() => {});
        this.loadAudioFiles();
      }

      if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
        navigator.vibrate(25);
      }
    } catch {
      // Ignore
    }
  }

  // Aliases ensuring every click route uses the provided touch_click sound
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

  public playChime() {
    this.playComplete();
  }
}

export const soundManager = new SoundManager();
export const AppSoundManager2D = soundManager;
