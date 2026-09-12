package com.wts.exam.service.impl;

import com.wts.exam.entity.ExamSubjectType;
import com.wts.exam.mapper.ExamSubjectTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectTypeServiceImplTest {

    @Mock
    private ExamSubjectTypeMapper typeMapper;
    @Mock
    private com.wts.auth.mapper.SysUserorgMapper userorgMapper;
    @Mock
    private com.wts.auth.mapper.SysOrganizationMapper organizationMapper;

    private SubjectTypeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubjectTypeServiceImpl(typeMapper, userorgMapper, organizationMapper);
    }

    @Test
    void createFillsLegacyRequiredPermissionColumns() {
        ExamSubjectType input = new ExamSubjectType();
        input.setName("数学");

        ExamSubjectType created = service.create(input, "teacher-1");

        ArgumentCaptor<ExamSubjectType> typeCaptor = ArgumentCaptor.forClass(ExamSubjectType.class);
        verify(typeMapper).insert(typeCaptor.capture());
        ExamSubjectType type = typeCaptor.getValue();
        assertEquals(created, type);
        assertNotNull(type.getId());
        assertEquals(type.getId(), type.getTreecode());
        assertEquals("1", type.getState());
        assertEquals("teacher-1", type.getCuser());
        assertEquals("teacher-1", type.getMuser());
        assertEquals("NONE", type.getParentid());
        assertEquals(1, type.getSort());
        assertEquals("1", type.getReadpop());
        assertEquals("1", type.getWritepop());
    }

    @Test
    void getTreeScopesByOwnersAndFillsOrgName() {
        ExamSubjectType root = type("type-1", "我的题库", "NONE", "user-1");
        ExamSubjectType child = type("type-2", "第一章", "type-1", "user-1");
        ExamSubjectType otherRoot = type("type-3", "他人题库", "NONE", "user-2");
        // 教师 user-1：只见自己的分类，含祖先链（自身即根）
        when(typeMapper.selectList(any())).thenReturn(List.of(root, child, otherRoot));
        when(userorgMapper.selectList(any())).thenReturn(List.of(userorg("user-1", "org-1")));
        when(organizationMapper.selectList(any())).thenReturn(List.of(org("org-1", "软件技术教研室")));

        List<ExamSubjectType> tree = service.getTree(List.of("user-1"));

        assertEquals(1, tree.size());
        assertEquals("type-1", tree.get(0).getId());
        assertEquals("软件技术教研室", tree.get(0).getOrgName());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals("第一章", tree.get(0).getChildren().get(0).getName());
    }

    @Test
    void getTreeKeepsAncestorChainForChildVisibleNode() {
        ExamSubjectType root = type("type-1", "共享根", "NONE", "user-9");
        ExamSubjectType child = type("type-2", "我的子分类", "type-1", "user-1");
        when(typeMapper.selectList(any())).thenReturn(List.of(root, child));

        List<ExamSubjectType> tree = service.getTree(List.of("user-1"));

        // 父链 type-1 保留为路径，但其下仅有可见子分类
        assertEquals(1, tree.size());
        assertEquals("type-1", tree.get(0).getId());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals("type-2", tree.get(0).getChildren().get(0).getId());
    }

    private static ExamSubjectType type(String id, String name, String parentId, String cuser) {
        ExamSubjectType type = new ExamSubjectType();
        type.setId(id);
        type.setName(name);
        type.setParentid(parentId);
        type.setCuser(cuser);
        type.setState("1");
        return type;
    }

    private static com.wts.auth.entity.SysUserorg userorg(String userId, String organizationId) {
        com.wts.auth.entity.SysUserorg userorg = new com.wts.auth.entity.SysUserorg();
        userorg.setUserid(userId);
        userorg.setOrganizationid(organizationId);
        return userorg;
    }

    private static com.wts.auth.entity.SysOrganization org(String id, String name) {
        com.wts.auth.entity.SysOrganization org = new com.wts.auth.entity.SysOrganization();
        org.setId(id);
        org.setName(name);
        org.setState("1");
        return org;
    }
}
