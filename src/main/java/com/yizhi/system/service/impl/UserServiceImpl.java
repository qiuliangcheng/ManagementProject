package com.yizhi.system.service.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.yizhi.common.config.yizhiConfig;
import com.yizhi.common.domain.FileDO;
import com.yizhi.common.domain.Tree;
import com.yizhi.common.domain.WeixinUserPrincipal;
import com.yizhi.common.service.FileService;
import com.yizhi.common.utils.*;
import com.yizhi.system.dao.DeptDao;
import com.yizhi.system.dao.UserDao;
import com.yizhi.system.dao.UserRoleDao;
import com.yizhi.system.domain.DeptDO;
import com.yizhi.system.domain.UserDO;
import com.yizhi.system.domain.UserRoleDO;
import com.yizhi.system.service.DeptService;
import com.yizhi.system.service.UserService;
import com.yizhi.system.vo.UserVO;

import javax.imageio.ImageIO;

@Transactional
@Service
public class UserServiceImpl implements UserService {
	@Autowired
	UserDao userMapper;
	@Autowired
	UserRoleDao userRoleMapper;
	@Autowired
	DeptDao deptMapper;
	@Autowired
	private FileService sysFileService;
	@Autowired
	private yizhiConfig yizhiConfig;
	@Autowired
    DeptService deptService;
	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	@Override
	public UserDO get(Long id) {
		List<Long> roleIds = userRoleMapper.listRoleId(id);
		UserDO user = userMapper.get(id);
		if(user.getDeptId()!=null) {
			DeptDO dep=deptMapper.get(user.getDeptId());
			if(dep!=null) {
				user.setDeptName(dep.getName());
			}
			
		}
		user.setroleIds(roleIds);
		return user;
	}

	@Override
	public List<UserDO> list(Map<String, Object> map) {
		 
	        if (map.get("deptId")!=null&&StringUtils.isNotBlank(map.get("deptId").toString())&&map.get("digui")!=null&&map.get("digui").toString().equals("1")) {
	        	String deptId = map.get("deptId").toString();
	        	Long deptIdl = Long.valueOf(deptId);
	            List<Long> childIds = deptService.listChildrenIds(deptIdl);
	            childIds.add(deptIdl);
	            map.put("deptId", null);
	            map.put("deptIds",childIds);
	        }
		return userMapper.list(map);
	}

	@Override
	public int count(Map<String, Object> map) {
		return userMapper.count(map);
	}

	@Transactional
	@Override
	public int save(UserDO user) {
		int count = userMapper.save(user);
		Long userId = user.getUserId();
		List<Long> roles = user.getroleIds();
		userRoleMapper.removeByUserId(userId);
		List<UserRoleDO> list = new ArrayList<>();
		for (Long roleId : roles) {
			UserRoleDO ur = new UserRoleDO();
			ur.setUserId(userId);
			ur.setRoleId(roleId);
			list.add(ur);
		}
		if (list.size() > 0) {
			userRoleMapper.batchSave(list);
		}
		return count;
	}

	@Override
	public int update(UserDO user) {
		int r = userMapper.update(user);
		Long userId = user.getUserId();
		List<Long> roles = user.getroleIds();
		userRoleMapper.removeByUserId(userId);
		List<UserRoleDO> list = new ArrayList<>();
		if(roles!=null&&roles.size()>0) {
			for (Long roleId : roles) {
				UserRoleDO ur = new UserRoleDO();
				ur.setUserId(userId);
				ur.setRoleId(roleId);
				list.add(ur);
			}
			if (list.size() > 0) {
				userRoleMapper.batchSave(list);
			}
		}
		
		
		return r;
	}

	@Override
	public int remove(Long userId) {
		userRoleMapper.removeByUserId(userId);
		return userMapper.remove(userId);
	}

	@Override
	public boolean exit(Map<String, Object> params) {
		boolean exit;
		exit = userMapper.list(params).size() > 0;
		return exit;
	}

	@Override
	public Set<String> listRoles(Long userId) {
		return null;
	}

	@Override
	public int resetPwd(UserVO userVO,UserDO userDO) throws Exception {
		if(Objects.equals(userVO.getUserDO().getUserId(),userDO.getUserId())){
			if(Objects.equals(MD5Utils.encrypt(userDO.getUsername(),userVO.getPwdOld()),userDO.getPassword())){
				userDO.setPassword(MD5Utils.encrypt(userDO.getUsername(),userVO.getPwdNew()));
				return userMapper.update(userDO);
			}else{
				throw new Exception("???????????????????????????");
			}
		}else{
			throw new Exception("???????????????????????????????????????");
		}
	}
	@Override
	public int adminResetPwd(UserVO userVO) throws Exception {
		UserDO userDO =get(userVO.getUserDO().getUserId());
		if("admin".equals(userDO.getUsername())){
			throw new Exception("????????????????????????????????????????????????");
		}
		userDO.setPassword(MD5Utils.encrypt(userDO.getUsername(), userVO.getPwdNew()));
		return userMapper.update(userDO);


	}

	@Transactional
	@Override
	public int batchremove(Long[] userIds) {
		int count = userMapper.batchRemove(userIds);
		userRoleMapper.batchRemoveByUserId(userIds);
		return count;
	}

	@Override
	public Tree<DeptDO> getTree() {
		List<Tree<DeptDO>> trees = new ArrayList<Tree<DeptDO>>();
		Map<String, Object> params=new HashMap<String, Object>();
		params.put("sort",BeanHump.camelToUnderline("order_num"));
		List<DeptDO> depts = deptMapper.list(params);
		Long[] pDepts = deptMapper.listParentDept();
		Long[] uDepts = userMapper.listAllDept();
		Long[] allDepts = (Long[]) ArrayUtils.addAll(pDepts, uDepts);
		for (DeptDO dept : depts) {
			if (!ArrayUtils.contains(allDepts, dept.getDeptId())) {
				continue;
			}
			Tree<DeptDO> tree = new Tree<DeptDO>();
			tree.setId(dept.getDeptId().toString());
			tree.setParentId(dept.getParentId().toString());
			tree.setText(dept.getName());
			Map<String, Object> state = new HashMap<>(16);
			state.put("opened", true);
			state.put("mType", "dept");
			tree.setState(state);
			trees.add(tree);
		}
		Map<String, Object> param=new HashMap<String, Object>(16);
		param.put("isvaild", 1);
		param.put("notssp", 1);//????????????
		List<UserDO> users = userMapper.list(param);
		for (UserDO user : users) {
			Tree<DeptDO> tree = new Tree<DeptDO>();
			tree.setId("u_"+user.getUserId().toString());
			tree.setParentId(user.getDeptId().toString());
			tree.setText(user.getName());
			Map<String, Object> state = new HashMap<>(16);
			state.put("opened", true);
			state.put("mType", "user");
			tree.setState(state);
			trees.add(tree);
		}
		// ????????????????????????????????????????????????????????????
		Tree<DeptDO> t = BuildTree.build(trees);
		return  t;
	}

	@Override
	public int updatePersonal(UserDO userDO) {
		return userMapper.update(userDO);
	}

    @Override
    public Map<String, Object> updatePersonalImg(MultipartFile file, String avatar_data, Long userId) throws Exception {
		String fileName = file.getOriginalFilename();
		fileName = FileUtil.renameToUUID(fileName);
		FileDO sysFile = new FileDO(FileType.fileType(fileName), "/files/" + fileName, new Date());
		//??????????????????
		String prefix = fileName.substring((fileName.lastIndexOf(".")+1));
		String[] str=avatar_data.split(",");
		//???????????????x??????
		int x = (int)Math.floor(Double.parseDouble(str[0].split(":")[1]));
		//???????????????y??????
		int y = (int)Math.floor(Double.parseDouble(str[1].split(":")[1]));
		//?????????????????????
		int h = (int)Math.floor(Double.parseDouble(str[2].split(":")[1]));
		//?????????????????????
		int w = (int)Math.floor(Double.parseDouble(str[3].split(":")[1]));
		//?????????????????????
		int r = Integer.parseInt(str[4].split(":")[1].replaceAll("}", ""));
		try {
			BufferedImage cutImage = ImageUtils.cutImage(file,x,y,w,h,prefix);
			BufferedImage rotateImage = ImageUtils.rotateImage(cutImage, r);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			boolean flag = ImageIO.write(rotateImage, prefix, out);
			//????????????????????????
			byte[] b = out.toByteArray();
			FileUtil.uploadFile(b, yizhiConfig.getUploadPath(), fileName);
		} catch (Exception e) {
			throw  new Exception("????????????????????????");
		}
		Map<String, Object> result = new HashMap<>();
		if(sysFileService.save(sysFile)>0){
			UserDO userDO = new UserDO();
			userDO.setUserId(userId);
			userDO.setPicId(sysFile.getId());
			if(userMapper.update(userDO)>0){
				result.put("url",sysFile.getUrl());
			}
		}
		return result;
    }
    
    @Transactional
	@Override
	public int updateBind(WeixinUserPrincipal member,UserDO oldUser) {
    	//?????????????????????????????????
    	userMapper.remove(Long.valueOf(member.getUserid()));
    	
    	//???????????????????????????????????????
    	UserRoleDO userRole=new UserRoleDO();
    	userRole.setNewUserId(Long.valueOf(member.getUserid()));
    	userRole.setUserId(oldUser.getUserId());
    	userRoleMapper.updateByUserId(userRole);
    	
    	//??????????????????ID??????????????????ID
    	oldUser.setNewUserId(Long.valueOf(member.getUserid()));
    	oldUser.setOpenid(member.getOpenId());
    	oldUser.setHeadimg(member.getHeadimg());
    	userMapper.update(oldUser);
    	
    	return 0;
	}

}
