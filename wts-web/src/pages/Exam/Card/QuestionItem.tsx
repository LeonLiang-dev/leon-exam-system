import React from 'react';
import { App, Radio, Checkbox, Input, Tag, Space, Button, Image as AntImage, Upload } from 'antd';
import { DeleteOutlined, UploadOutlined } from '@ant-design/icons';

const TIPTYPE_LABELS: Record<string, { label: string; color: string }> = {
  '1': { label: '填空题', color: 'blue' },
  '2': { label: '单选题', color: 'green' },
  '3': { label: '多选题', color: 'orange' },
  '4': { label: '判断题', color: 'purple' },
  '5': { label: '主观题', color: 'cyan' },
};

interface AnswerOption {
  id: string;
  answer: string;
  sort: number;
  pcontent?: string;
}

interface Subject {
  paperSubjectId: string;
  subjectId: string;
  versionId: string;
  point: number;
  introduction?: string;
  tiptype: string;
  tipstr?: string;
  tipnote?: string;
  pcontent?: string;
  answers: AnswerOption[];
}

interface Props {
  index: number;
  subject: Subject;
  value?: any;
  onChange?: (value: any) => void;
  mobile?: boolean;
}

const MAX_IMAGE_DATA_URL_LENGTH = 8 * 1024 * 1024;
const MAX_IMAGE_SIDE = 1400;

const compressImageToDataUrl = (file: File): Promise<string> => new Promise((resolve, reject) => {
  if (!/^image\/(jpeg|jpg|png|webp)$/i.test(file.type)) {
    reject(new Error('仅支持 JPG、JPEG、PNG、WEBP 图片'));
    return;
  }
  const reader = new FileReader();
  reader.onerror = () => reject(new Error('读取图片失败'));
  reader.onload = () => {
    const img = new window.Image();
    img.onerror = () => reject(new Error('图片解析失败'));
    img.onload = () => {
      const scale = Math.min(1, MAX_IMAGE_SIDE / img.width, MAX_IMAGE_SIDE / img.height);
      const width = Math.max(1, Math.round(img.width * scale));
      const height = Math.max(1, Math.round(img.height * scale));
      const canvas = document.createElement('canvas');
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        reject(new Error('浏览器不支持图片处理'));
        return;
      }
      ctx.fillStyle = '#fff';
      ctx.fillRect(0, 0, width, height);
      ctx.drawImage(img, 0, 0, width, height);
      const dataUrl = canvas.toDataURL('image/jpeg', 0.82);
      if (dataUrl.length > MAX_IMAGE_DATA_URL_LENGTH) {
        reject(new Error('图片过大，请裁剪或压缩后再粘贴'));
        return;
      }
      resolve(dataUrl);
    };
    img.src = String(reader.result || '');
  };
  reader.readAsDataURL(file);
});

const getPastedImageFile = (event: React.ClipboardEvent): File | null => {
  const items = Array.from(event.clipboardData?.items || []);
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      return item.getAsFile();
    }
  }
  return null;
};

const QuestionItem: React.FC<Props> = ({ index, subject, value, onChange, mobile }) => {
  const { message } = App.useApp();
  const typeInfo = TIPTYPE_LABELS[subject.tiptype] || { label: '未知', color: 'default' };

  const getSubjectiveValue = () => {
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      return { text: value.text || '', images: Array.isArray(value.images) ? value.images : [] };
    }
    if (typeof value === 'string') {
      return { text: value, images: [] };
    }
    return { text: '', images: [] };
  };

  const updateSubjectiveValue = (patch: Partial<{ text: string; images: string[] }>) => {
    const current = getSubjectiveValue();
    onChange?.({ ...current, ...patch });
  };

  const appendSubjectiveImage = async (file: File) => {
    const current = getSubjectiveValue();
    if (current.images.length >= 5) {
      message.warning('最多上传5张图片');
      return;
    }
    const hide = message.loading('正在处理图片...', 0);
    try {
      const dataUrl = await compressImageToDataUrl(file);
      updateSubjectiveValue({ images: [...current.images, dataUrl] });
      message.success('图片已添加');
    } catch (error: any) {
      message.error(error?.message || '图片处理失败');
    } finally {
      hide();
    }
  };

  const handleSubjectivePaste = async (event: React.ClipboardEvent) => {
    const file = getPastedImageFile(event);
    if (!file) return;
    event.preventDefault();
    await appendSubjectiveImage(file);
  };

  const renderQuestionBody = () => {
    switch (subject.tiptype) {
      case '2': // Single choice
        return (
          <Radio.Group
            value={value}
            onChange={(e) => onChange?.(e.target.value)}
          >
            <Space direction="vertical" style={{ width: '100%' }}>
              {subject.answers.map((a) => (
                <Radio key={a.id} value={a.id} style={{ lineHeight: 2 }}>
                  {a.answer}
                </Radio>
              ))}
            </Space>
          </Radio.Group>
        );

      case '3': // Multiple choice
        return (
          <Checkbox.Group
            value={value || []}
            onChange={(checkedValues) => onChange?.(checkedValues)}
          >
            <Space direction="vertical" style={{ width: '100%' }}>
              {subject.answers.map((a) => (
                <Checkbox key={a.id} value={a.id} style={{ lineHeight: 2 }}>
                  {a.answer}
                </Checkbox>
              ))}
            </Space>
          </Checkbox.Group>
        );

      case '4': // True/False
        return (
          <Radio.Group
            value={value}
            onChange={(e) => onChange?.(e.target.value)}
          >
            <Space direction="vertical">
              {subject.answers.map((a) => (
                <Radio key={a.id} value={a.id} style={{ lineHeight: 2 }}>
                  {a.answer}
                </Radio>
              ))}
            </Space>
          </Radio.Group>
        );

      case '1': // Fill in the blank
        return (
          <Space direction="vertical" style={{ width: '100%' }}>
            {subject.answers.map((a) => {
              const currentValue = value?.[a.id] || '';
              return (
                <div key={a.id} style={{ marginBottom: 10, display: 'flex', alignItems: 'flex-start', gap: 8 }}>
                  <span style={{ flexShrink: 0, lineHeight: '32px' }}>({a.sort})</span>
                  <Input
                    style={{ flex: 1 }}
                    maxLength={100}
                    placeholder="请输入文本答案"
                    value={currentValue}
                    onChange={(e) => {
                      const newValue = { ...(value || {}), [a.id]: e.target.value };
                      onChange?.(newValue);
                    }}
                  />
                </div>
              );
            })}
          </Space>
        );

      case '5': // Essay
        {
          const current = getSubjectiveValue();
          return (
            <Space direction="vertical" style={{ width: '100%' }}>
              <Input.TextArea
                rows={6}
                maxLength={10000}
                showCount
                placeholder="请输入文字答案"
                value={current.text}
                onPaste={handleSubjectivePaste}
                onChange={(e) => updateSubjectiveValue({ text: e.target.value })}
              />
              <Upload
                accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
                showUploadList={false}
                beforeUpload={async (file) => {
                  await appendSubjectiveImage(file as File);
                  return false;
                }}
              >
                <Button icon={<UploadOutlined />} disabled={current.images.length >= 5}>
                  添加图片
                </Button>
              </Upload>
              {current.images.length > 0 && (
                <Space wrap align="start">
                  {current.images.map((src: string, imgIndex: number) => (
                    <div key={`${src.slice(0, 32)}-${imgIndex}`} style={{ position: 'relative' }}>
                      <AntImage
                        src={src}
                        alt={`主观题答案图片${imgIndex + 1}`}
                        style={{
                          maxWidth: mobile ? 120 : 160,
                          maxHeight: 120,
                          borderRadius: 4,
                          objectFit: 'contain',
                        }}
                      />
                      <Button
                        size="small"
                        danger
                        icon={<DeleteOutlined />}
                        style={{ marginTop: 6, display: 'block' }}
                        onClick={() => updateSubjectiveValue({
                          images: current.images.filter((_: string, idx: number) => idx !== imgIndex),
                        })}
                      >
                        删除
                      </Button>
                    </div>
                  ))}
                </Space>
              )}
            </Space>
          );
        }

      default:
        return <div>不支持的题型</div>;
    }
  };

  return (
    <div
      id={`question-${index}`}
      className="wts-question-item"
    >
      <div className="wts-question-header">
        <span className="wts-question-number">{index + 1}</span>
        <Tag color={typeInfo.color}>{typeInfo.label}</Tag>
        <span className="wts-question-text">
          {subject.tipstr || subject.introduction || ''}
        </span>
        <span className="wts-question-points">({subject.point}分)</span>
      </div>
      {subject.tipnote && (
        <div style={{ marginBottom: 12, color: '#666', fontSize: 13 }}>{subject.tipnote}</div>
      )}
      {subject.pcontent && (
        <div style={{ marginBottom: 12 }}>
          <AntImage
            src={subject.pcontent}
            alt="题干配图"
            style={{
              maxWidth: mobile ? 260 : 420,
              maxHeight: 260,
              borderRadius: 4,
              objectFit: 'contain',
            }}
          />
        </div>
      )}
      {renderQuestionBody()}
    </div>
  );
};

export default QuestionItem;
