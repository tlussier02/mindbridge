-- Cognitive Distortion Definitions
MERGE INTO cognitive_distortions (id, name, description) KEY(id) VALUES
('all-or-nothing', 'All-or-Nothing Thinking', 'Seeing things in black-and-white categories with no middle ground.');
MERGE INTO cognitive_distortions (id, name, description) KEY(id) VALUES
('catastrophizing', 'Catastrophizing', 'Expecting the worst possible outcome without considering other possibilities.');
MERGE INTO cognitive_distortions (id, name, description) KEY(id) VALUES
('mind-reading', 'Mind Reading', 'Assuming you know what others are thinking without evidence.');
MERGE INTO cognitive_distortions (id, name, description) KEY(id) VALUES
('overgeneralization', 'Overgeneralization', 'Making broad conclusions based on a single event.');
MERGE INTO cognitive_distortions (id, name, description) KEY(id) VALUES
('should-statements', 'Should Statements', 'Using rigid rules about how you or others should behave.');
MERGE INTO cognitive_distortions (id, name, description) KEY(id) VALUES
('mental-filtering', 'Mental Filtering', 'Focusing exclusively on negative details while ignoring positives.');
MERGE INTO cognitive_distortions (id, name, description) KEY(id) VALUES
('fortune-telling', 'Fortune Telling', 'Predicting negative outcomes without sufficient evidence.');
MERGE INTO cognitive_distortions (id, name, description) KEY(id) VALUES
('emotional-reasoning', 'Emotional Reasoning', 'Assuming that negative feelings reflect reality.');
MERGE INTO cognitive_distortions (id, name, description) KEY(id) VALUES
('labeling', 'Labeling', 'Attaching a fixed, global label to yourself or others based on limited evidence.');
MERGE INTO cognitive_distortions (id, name, description) KEY(id) VALUES
('personalization', 'Personalization', 'Blaming yourself for events outside your control.');

-- Distortion Examples
MERGE INTO distortion_examples (cognitive_distortion_id, examples) KEY(cognitive_distortion_id, examples) VALUES
('all-or-nothing', 'If I am not perfect, I am a total failure.');
MERGE INTO distortion_examples (cognitive_distortion_id, examples) KEY(cognitive_distortion_id, examples) VALUES
('all-or-nothing', 'Either I do it right or not at all.');
MERGE INTO distortion_examples (cognitive_distortion_id, examples) KEY(cognitive_distortion_id, examples) VALUES
('catastrophizing', 'If I make a mistake, I will definitely get fired.');
MERGE INTO distortion_examples (cognitive_distortion_id, examples) KEY(cognitive_distortion_id, examples) VALUES
('catastrophizing', 'This headache must be a brain tumor.');
MERGE INTO distortion_examples (cognitive_distortion_id, examples) KEY(cognitive_distortion_id, examples) VALUES
('mind-reading', 'They must think I am incompetent.');
MERGE INTO distortion_examples (cognitive_distortion_id, examples) KEY(cognitive_distortion_id, examples) VALUES
('mind-reading', 'She did not smile because she hates me.');
MERGE INTO distortion_examples (cognitive_distortion_id, examples) KEY(cognitive_distortion_id, examples) VALUES
('overgeneralization', 'I failed once, so I will always fail.');
MERGE INTO distortion_examples (cognitive_distortion_id, examples) KEY(cognitive_distortion_id, examples) VALUES
('overgeneralization', 'Nobody ever listens to me.');
MERGE INTO distortion_examples (cognitive_distortion_id, examples) KEY(cognitive_distortion_id, examples) VALUES
('should-statements', 'I should always be productive.');
MERGE INTO distortion_examples (cognitive_distortion_id, examples) KEY(cognitive_distortion_id, examples) VALUES
('should-statements', 'They should know how I feel.');

-- Session Modules
MERGE INTO session_modules (id, name, description, category, order_index) KEY(id) VALUES
('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Understanding Burnout', 'Learn to recognize the signs and stages of workplace burnout through a CBT lens.', 'Foundation', 1);
MERGE INTO session_modules (id, name, description, category, order_index) KEY(id) VALUES
('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Cognitive Restructuring', 'Identify and challenge negative thought patterns that contribute to burnout.', 'Core Skills', 2);
MERGE INTO session_modules (id, name, description, category, order_index) KEY(id) VALUES
('c3d4e5f6-a7b8-9012-cdef-123456789012', 'Behavioral Activation', 'Develop healthy behavioral patterns and re-engage with meaningful activities.', 'Core Skills', 3);
MERGE INTO session_modules (id, name, description, category, order_index) KEY(id) VALUES
('d4e5f6a7-b8c9-0123-defa-234567890123', 'Stress Management', 'Learn practical stress management and relaxation techniques.', 'Wellness', 4);
MERGE INTO session_modules (id, name, description, category, order_index) KEY(id) VALUES
('e5f6a7b8-c9d0-1234-efab-345678901234', 'Work-Life Balance', 'Establish healthy boundaries and sustainable work practices.', 'Recovery', 5);

-- CBT Sessions
MERGE INTO cbt_sessions (id, module_id, title, description, duration_minutes, order_index) KEY(id) VALUES
('11111111-1111-1111-1111-111111111111', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'What is Burnout?', 'Explore the definition, causes, and stages of burnout using the Maslach model.', 30, 1);
MERGE INTO cbt_sessions (id, module_id, title, description, duration_minutes, order_index) KEY(id) VALUES
('22222222-2222-2222-2222-222222222222', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Recognizing Your Signs', 'Identify your personal burnout warning signs and triggers.', 25, 2);
MERGE INTO cbt_sessions (id, module_id, title, description, duration_minutes, order_index) KEY(id) VALUES
('33333333-3333-3333-3333-333333333333', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Identifying Thought Patterns', 'Learn to recognize automatic negative thoughts related to work.', 35, 1);
MERGE INTO cbt_sessions (id, module_id, title, description, duration_minutes, order_index) KEY(id) VALUES
('44444444-4444-4444-4444-444444444444', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Challenging Distortions', 'Practice techniques to challenge and reframe cognitive distortions.', 40, 2);
MERGE INTO cbt_sessions (id, module_id, title, description, duration_minutes, order_index) KEY(id) VALUES
('55555555-5555-5555-5555-555555555555', 'c3d4e5f6-a7b8-9012-cdef-123456789012', 'Activity Scheduling', 'Create a balanced activity schedule that includes pleasurable and mastery activities.', 30, 1);
MERGE INTO cbt_sessions (id, module_id, title, description, duration_minutes, order_index) KEY(id) VALUES
('66666666-6666-6666-6666-666666666666', 'd4e5f6a7-b8c9-0123-defa-234567890123', 'Relaxation Techniques', 'Learn progressive muscle relaxation and breathing exercises.', 25, 1);
MERGE INTO cbt_sessions (id, module_id, title, description, duration_minutes, order_index) KEY(id) VALUES
('77777777-7777-7777-7777-777777777777', 'e5f6a7b8-c9d0-1234-efab-345678901234', 'Setting Boundaries', 'Develop skills for setting healthy workplace boundaries.', 35, 1);
