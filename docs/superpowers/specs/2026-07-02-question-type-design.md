# Question Type Design

## Goal

Implement the V1.1 question-type specification from `题型设计规范.md` as the single source of truth for question authoring, answering, grading, and review.

The system will support only these five question types:

1. Fill in the blank
2. Single choice
3. Multiple choice
4. True or false
5. Subjective question

Legacy exam business data does not need compatibility. Legacy attachment questions and historical exam/question/answer data should be removed from clean initialization and cleared by a migration for existing local deployments. User, organization, authentication, and permission seed data are outside this cleanup scope.

## Scope

Backend:

- Enforce question authoring rules before creating a subject version.
- Normalize answer structures for each question type.
- Score objective questions automatically.
- Auto-match fill-in blanks first, then keep the card pending manual review when any fill-in blank is unmatched.
- Allow manual review only for subjective questions and fill-in questions that require review.
- Remove attachment-question creation from the supported type set.
- Add a database migration that clears existing exam business data and removes unsupported attachment-question state.

Frontend:

- Update question management UI so each type has only the fields allowed by the spec.
- Update student answering UI so each type only permits its allowed input mode.
- Remove image paste/upload support from fill-in blanks.
- Add text plus image answering for subjective questions.
- Update review and result pages to display pending fill-in review and subjective image answers.

## Data Cleanup

The cleanup migration should delete exam lifecycle data in dependency order:

- `wts_card_answer`
- `wts_card_point`
- `wts_card`
- room participant and room-paper relation tables
- `wts_room`
- paper-subject and chapter tables
- `wts_paper`
- subject answers and versions
- `wts_subject`
- all random paper generation rules and steps

It should not delete:

- users
- organizations
- roles, menus, permissions, or auth data
- subject category tree

Clean SQL seed files should also contain no legacy question, paper, room, card, or attachment-question sample data.

## Question Authoring Rules

Each subject requires:

- Non-empty question stem.
- Score greater than 0.
- Optional question image stored in `wts_subject_version.PCONTENT` as a compressed data URL. V1 does not introduce a file service.

Single choice:

- 2 to 10 options.
- Every option has non-empty text.
- Exactly one correct option.

Multiple choice:

- 2 to 10 options.
- At least two correct options.
- At least one incorrect option.

True or false:

- Exactly two fixed options: `正确` and `错误`.
- Exactly one correct option.

Fill in the blank:

- At least one blank.
- Each blank has at least one standard answer.
- Multiple accepted answers for a blank are stored using the existing pipe-separated answer text.
- Each blank has a positive point weight or receives an equal default share.

Subjective:

- Requires only stem and score.
- Optional teacher-only scoring standard and sample answer can be stored in the existing note fields.
- One answer placeholder is created so student answers can bind to the version.

## Answering Rules

Single choice and true/false:

- Student selects one option.
- Empty selection is allowed until submit and counts as unanswered.

Multiple choice:

- Student selects zero or more options.
- Full-match scoring only.

Fill in the blank:

- Student enters plain text only.
- Each blank max length is 100 characters.
- Image upload, image paste, rich text, and file upload are disallowed.

Subjective:

- Student can submit text, images, or both.
- Text max length is 10000 characters.
- Images are compressed client-side and stored in the answer payload as data URLs for V1.
- Image formats: JPG, JPEG, PNG, WEBP.
- Image count is limited to 0 to 5.

All answers remain editable until submit. Submitted cards cannot be modified.

## Grading Rules

Objective grading:

- Single choice: selected answer must be the correct option.
- Multiple choice: selected set must exactly match the correct set.
- True/false: selected answer must be the correct option.

Fill-in grading:

- Trim leading/trailing whitespace.
- Match case-insensitively by default.
- A blank is correct if it matches any accepted answer for that blank.
- Matched blanks receive their blank weight.
- If all blanks match, the card can be auto-judged if no subjective question exists.
- If any answered blank fails to match, the card remains pending manual review.

Manual review:

- Subjective questions are always reviewable.
- Fill-in questions are reviewable when auto matching leaves an unmatched blank.
- Objective questions are never manually editable.
- Manual score must be between 0 and the question max score.
- Final card score is the sum of all question scores.

## Backend Components

Add or refactor:

- `SubjectRuleValidator`: validates `SubjectDTO` by tip type.
- `QuestionTypeRules`: central constants for the five supported types and manual-review decisions.
- `CardAnswerGrader`: extend from weight-only scoring to expose fill-in review state.
- `CardServiceImpl.autoGrade`: mark cards pending manual review when fill-in questions need teacher action.
- `CardServiceImpl.judge`: permit subjective and review-required fill-in questions only.

DTO/entity changes:

- Keep existing answer tables for V1.
- Add `REVIEW_REQUIRED varchar(1)` and `REVIEW_REASON varchar(64)` to `wts_card_point`.
- Add `REVIEW_COMMENT varchar(512)` to `wts_card_point` and `wts_card_answer` for manual-review comments.
- Store subjective answer payload in `wts_card_answer.VALSTR` as JSON:

```json
{"text":"answer text","images":["data:image/jpeg;base64,..."]}
```

Migration:

- Add columns needed for review state.
- Clear existing exam business data.
- Leave auth/system seed data intact.

## Frontend Components

Question management:

- Replace the generic answer list with type-specific editors.
- Remove attachment type from the type selector.
- Generate true/false options automatically.
- For fill-in blanks, show blank rows with accepted-answer tags or pipe-separated text.
- For subjective questions, show scoring standard and sample-answer fields.

Student card page:

- Single choice and true/false use `Radio.Group`.
- Multiple choice uses `Checkbox.Group`.
- Fill-in uses plain `Input` without image paste handling.
- Subjective uses `Input.TextArea` plus image uploader/paste support.
- Submit payload serializes subjective answer JSON into one answer row.

Review page:

- Display objective scores as read-only.
- Display fill-in answer, standard answers, and review-required status.
- Allow score editing for subjective questions and review-required fill-in questions.
- Render subjective text and images.

Result page:

- Show pending review for submitted cards.
- Render subjective images.
- Show final manual score after judging.

## Testing

Backend unit tests:

- Subject validation for all five types.
- Rejection of unsupported attachment type on new create/update.
- Single/multiple/true-false grading.
- Fill-in full match auto-judges.
- Fill-in partial/unmatched answer requires manual review.
- Manual judging rejects objective questions and accepts review-required fill-in questions.
- Cleanup migration is syntactically valid against the schema.

Frontend validation:

- Type-specific form validation prevents invalid authoring payloads.
- Fill-in answer input cannot accept image paste/upload.
- Subjective answer serializes and restores text plus images.

Verification commands:

- `mvn -pl wts-app -am -DskipTests compile`
- targeted `mvn -pl wts-app -am -Dtest=SubjectServiceImplTest,CardAnswerGraderTest,CardServiceImplLifecycleTest,RandomServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `./node_modules/.bin/max lint`
- `./node_modules/.bin/max build` if local font/network constraints permit.
