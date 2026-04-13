'use client';

import { useEffect } from 'react';
import { BriefingFlow } from '@/components/briefing/BriefingFlow';
import { useBriefingStore } from '@/stores/useBriefingStore';

interface BriefingPageClientProps {
  token: string;
  sessionId: string;
}

export function BriefingPageClient({
  token,
  sessionId,
}: BriefingPageClientProps) {
  const setSessionId = useBriefingStore((s) => s.setSessionId);

  // Set sessionId na store quando o componente montar
  useEffect(() => {
    setSessionId(sessionId);
  }, [sessionId, setSessionId]);

  return <BriefingFlow token={token} />;
}
