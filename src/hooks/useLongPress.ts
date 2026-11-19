import React, { useRef, useCallback } from 'react';

interface UseLongPressOptions {
  delay?: number;
  moveThreshold?: number;
}

export function useLongPress(
  onLongPress: () => void,
  onClick?: () => void,
  options: UseLongPressOptions = {}
) {
  const { delay = 480, moveThreshold = 10 } = options;
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const longPressTriggeredRef = useRef(false);
  const startPosRef = useRef<{ x: number; y: number } | null>(null);

  const clear = useCallback(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  const handleTouchStart = useCallback(
    (e: React.TouchEvent) => {
      longPressTriggeredRef.current = false;
      const touch = e.touches[0];
      startPosRef.current = touch ? { x: touch.clientX, y: touch.clientY } : null;
      clear();
      timerRef.current = setTimeout(() => {
        longPressTriggeredRef.current = true;
        onLongPress();
      }, delay);
    },
    [onLongPress, delay, clear]
  );

  const handleTouchMove = useCallback(
    (e: React.TouchEvent) => {
      if (!startPosRef.current || !timerRef.current) return;
      const touch = e.touches[0];
      if (!touch) return;
      const dx = Math.abs(touch.clientX - startPosRef.current.x);
      const dy = Math.abs(touch.clientY - startPosRef.current.y);
      if (dx > moveThreshold || dy > moveThreshold) {
        clear();
      }
    },
    [moveThreshold, clear]
  );

  const handleTouchEnd = useCallback(() => {
    clear();
  }, [clear]);

  const handleMouseDown = useCallback(
    (e: React.MouseEvent) => {
      if (e.button !== 0) return;
      longPressTriggeredRef.current = false;
      startPosRef.current = { x: e.clientX, y: e.clientY };
      clear();
      timerRef.current = setTimeout(() => {
        longPressTriggeredRef.current = true;
        onLongPress();
      }, delay);
    },
    [onLongPress, delay, clear]
  );

  const handleMouseMove = useCallback(
    (e: React.MouseEvent) => {
      if (!startPosRef.current || !timerRef.current) return;
      const dx = Math.abs(e.clientX - startPosRef.current.x);
      const dy = Math.abs(e.clientY - startPosRef.current.y);
      if (dx > moveThreshold || dy > moveThreshold) {
        clear();
      }
    },
    [moveThreshold, clear]
  );

  const handleMouseUp = useCallback(() => {
    clear();
  }, [clear]);

  const handleMouseLeave = useCallback(() => {
    clear();
  }, [clear]);

  const handleContextMenu = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      clear();
      longPressTriggeredRef.current = true;
      onLongPress();
    },
    [onLongPress, clear]
  );

  const handleClick = useCallback(
    (e: React.MouseEvent) => {
      if (longPressTriggeredRef.current) {
        e.preventDefault();
        e.stopPropagation();
        longPressTriggeredRef.current = false;
        return;
      }
      onClick?.();
    },
    [onClick]
  );

  return {
    onTouchStart: handleTouchStart,
    onTouchMove: handleTouchMove,
    onTouchEnd: handleTouchEnd,
    onTouchCancel: handleTouchEnd,
    onMouseDown: handleMouseDown,
    onMouseMove: handleMouseMove,
    onMouseUp: handleMouseUp,
    onMouseLeave: handleMouseLeave,
    onContextMenu: handleContextMenu,
    onClick: handleClick,
  };
}
