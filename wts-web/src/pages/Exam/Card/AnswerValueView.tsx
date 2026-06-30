import React from 'react';
import { Image, Tag } from 'antd';

export const isImageAnswerValue = (value?: unknown): value is string =>
  typeof value === 'string' && /^data:image\/[a-zA-Z0-9.+-]+;base64,/.test(value);

const SELECTION_TIPTYPES = new Set(['2', '3', '4']);

export const getCardAnswerDisplayValue = (cardAnswer: any, subject?: any): string | undefined => {
  const value = cardAnswer?.valstr;
  if (!subject || !SELECTION_TIPTYPES.has(subject.tiptype) || value !== 'true') {
    return value;
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

interface Props {
  value?: string;
  block?: boolean;
}

const AnswerValueView: React.FC<Props> = ({ value, block }) => {
  if (!value) {
    return <span style={{ color: '#999' }}>未作答</span>;
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
