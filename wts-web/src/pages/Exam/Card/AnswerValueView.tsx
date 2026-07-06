import React from 'react';
import { Image, Tag } from 'antd';

export const isImageAnswerValue = (value?: unknown): value is string =>
  typeof value === 'string' && /^data:image\/[a-zA-Z0-9.+-]+;base64,/.test(value);

const SELECTION_TIPTYPES = new Set(['2', '3', '4']);

export const getCardAnswerDisplayValue = (cardAnswer: any, subject?: any): string | undefined => {
  const value = cardAnswer?.valstr;
  const tiptype = subject?.tiptype != null ? String(subject.tiptype) : undefined;
  if (!subject || !tiptype || !SELECTION_TIPTYPES.has(tiptype)) {
    return value || undefined;
  }

  if (value !== 'true') {
    return undefined;
  }

  const answerId = cardAnswer.answerid || cardAnswer.answerId;
  const answerIndex = (subject.answers || []).findIndex((answer: any) => answer.id === answerId);
  const answerOption = answerIndex >= 0 ? subject.answers[answerIndex] : null;
  if (!answerOption) {
    return answerId || '已选择';
  }

  const label = answerIndex >= 0 && answerIndex < 26
    ? `${String.fromCharCode(65 + answerIndex)}. `
    : '';
  return `${label}${answerOption.answer || answerOption.pcontent || answerOption.id}`;
};

export const getCardAnswerDisplayValues = (cardAnswers: any[] = [], subject?: any): string[] =>
  cardAnswers
    .map((cardAnswer) => getCardAnswerDisplayValue(cardAnswer, subject))
    .filter((value): value is string => Boolean(value));

interface Props {
  value?: string;
  block?: boolean;
}

const AnswerValueView: React.FC<Props> = ({ value, block }) => {
  if (!value) {
    return <span style={{ color: '#999' }}>未作答</span>;
  }

  try {
    const parsed = JSON.parse(value);
    if (parsed && typeof parsed === 'object' && ('text' in parsed || 'images' in parsed)) {
      const images = Array.isArray(parsed.images) ? parsed.images : [];
      const text = parsed.text || '';
      if (!text && images.length === 0) {
        return <span style={{ color: '#999' }}>未作答</span>;
      }
      return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {text && <div style={{ whiteSpace: 'pre-wrap' }}>{text}</div>}
          {images.length > 0 && (
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {images.map((src: string, index: number) => (
                <Image
                  key={`${src.slice(0, 32)}-${index}`}
                  src={src}
                  alt={`主观题答案图片${index + 1}`}
                  style={{
                    maxWidth: block ? 360 : 180,
                    maxHeight: block ? 260 : 120,
                    borderRadius: 6,
                    border: '1px solid #e5e7eb',
                    objectFit: 'contain',
                    background: '#fff',
                  }}
                />
              ))}
            </div>
          )}
        </div>
      );
    }
  } catch {
    // Non-JSON answers are displayed below.
  }

  if (isImageAnswerValue(value)) {
    return (
      <Image
        src={value}
        alt="粘贴的答案图片"
        style={{
          maxWidth: block ? 360 : 180,
          maxHeight: block ? 260 : 120,
          borderRadius: 6,
          border: '1px solid #e5e7eb',
          objectFit: 'contain',
          background: '#fff',
        }}
      />
    );
  }

  if (block) {
    return <div style={{ whiteSpace: 'pre-wrap' }}>{value}</div>;
  }

  return <Tag style={{ marginRight: 4, whiteSpace: 'normal' }}>{value}</Tag>;
};

export default AnswerValueView;
