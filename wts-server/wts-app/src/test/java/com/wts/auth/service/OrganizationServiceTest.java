package com.wts.auth.service;

import com.wts.auth.entity.SysOrganization;
import com.wts.auth.mapper.SysOrganizationMapper;
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
class OrganizationServiceTest {

    @Mock
    private SysOrganizationMapper organizationMapper;
    @Mock
    private com.wts.auth.mapper.SysUserorgMapper userorgMapper;

    private OrganizationService service;

    @BeforeEach
    void setUp() {
        service = new OrganizationService(organizationMapper, userorgMapper);
    }

    @Test
    void createRootOrganizationNormalizesBlankParentToNone() {
        SysOrganization organization = new SysOrganization();
        organization.setName("高一");

        service.createOrganization(organization, "admin-1");

        ArgumentCaptor<SysOrganization> captor = ArgumentCaptor.forClass(SysOrganization.class);
        verify(organizationMapper).insert(captor.capture());
        SysOrganization saved = captor.getValue();
        assertEquals("NONE", saved.getParentid());
        assertEquals("1", saved.getState());
        assertEquals("0", saved.getType());
        assertEquals(1, saved.getSort());
        assertNotNull(saved.getTreecode());
    }

    @Test
    void getOrgTreeTreatsNullParentAsRoot() {
        SysOrganization root = new SysOrganization();
        root.setId("org-1");
        root.setName("高一");
        root.setState("1");
        root.setParentid(null);
        root.setSort(1);
        when(organizationMapper.selectList(any())).thenReturn(List.of(root));

        List<OrganizationService.OrgTreeNode> tree = service.getOrgTree();

        assertEquals(1, tree.size());
        assertEquals("org-1", tree.get(0).getId());
        assertEquals("高一", tree.get(0).getName());
        assertEquals("NONE", tree.get(0).getParentid());
    }
}
