package cn.sowell.zhsq.urp.workbench.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.util.StringUtils;
import org.trundle.action.Rundata;
import org.trundle.common.Debuger;
import org.trundle.db.HibernateUtils;

import cn.sowell.zhsq.common.accessory.pojo.Accessory;
import cn.sowell.zhsq.common.accessory.service.AccessoryService;
import cn.sowell.zhsq.common.service.BaseService;
import cn.sowell.zhsq.common.util.DateFormatUtil;
import cn.sowell.zhsq.doc.util.FileUtil;
import cn.sowell.zhsq.enchiridion.Constants;
import cn.sowell.zhsq.urp.appmaintain.pojo.CredentialsModule;
import cn.sowell.zhsq.urp.appmaintain.service.CredentialsModuleService;
import cn.sowell.zhsq.urp.urp.pojo.Unit;
import cn.sowell.zhsq.urp.workbench.Constant;
import cn.sowell.zhsq.urp.workbench.pojo.Certificate;
import cn.sowell.zhsq.urp.workbench.pojo.CertificateFJ;
123213
public class CertificateService extends BaseService implements Constant, Constants{
	
	private static final Log logger = Debuger.getDebuger(AccountBookService.class);
	
	private CredentialsModuleService credentialsModuleService;

	/**
	 * @param criteria Certificate����
	 * @param user
	 * @param state ����״̬
	 * @return
	 */
	public List<Certificate> list(Certificate criteria, Unit user, String state) {
		String hql = " select * from t_workbench_certificate c where state ='" + state + "'";
		if (StringUtils.hasText(criteria.getUserName())) {
			hql += " and user_name like '%" + criteria.getUserName() + "%'";
		}
		if (StringUtils.hasText(criteria.getTypeName())) {
			hql += " and type_name like '%" + criteria.getTypeName() + "%'";
		}
		if (StringUtils.hasText(criteria.getAreaCode())) {
			hql += " and area_code like '%" + criteria.getAreaCode() + "%' ";
		}
		hql +=" and c.id not in(select i.busid from t_flow_instance i)";
		hql += " and operator_id='" + user.getId() + "'";
		// hql +=" and operator_id in " + unitService.getManagedUnits(user);
		hql += " order by c.operate_time desc";
		return getPageListBySql(hql, criteria, Certificate.class);
	}

	/**
	 * ����֤����ѯ
	 * 
	 * @param criteria
	 * @param flowsql
	 * @param userid
	 * @return
	 */
	public List<Certificate> dlbj_list(Certificate criteria, String flowsql, String userid) {
		// ���ݹ������ҳ����Դ��������
		String hql = "select * from (select certificate.* from t_workbench_certificate certificate,"
				+ flowsql;
		hql += " ) c  where state != '" + Constant.CERTIFICATE_STATE_END 
				+"' and state != '" + Constant.CERTIFICATE_STATE_SUBMITTED + "'"; //�����ֵ�����֤��״̬Ϊ c0
		if (StringUtils.hasText(criteria.getUserName())) {
			hql += " and user_name like '%" + criteria.getUserName() + "%'";
		}
		if (StringUtils.hasText(criteria.getTypeName())) {
			hql += " and type_name like '%" + criteria.getTypeName() + "%'";
		}
		if (StringUtils.hasText(criteria.getAreaCode())) {
			hql += " and area_code like '%" + criteria.getAreaCode() + "%' ";
		}
		hql += " order by c.operate_time desc";
		return getPageListBySql(hql, criteria, Certificate.class);
	}
	
	/**
	 * �����б�
	 * @param criteria
	 */
	public List<Certificate> superviseList(Certificate criteria,Unit unit) {
		String hql = "select c from Certificate c,DealRecord d where d.problemId=c.id"
				+" and d.operateType = '����' and c.operatorId='" + unit.getId() + "'";
		return getPageList(hql, criteria);
	}
	
	/**
	 * ��Ҫȡ֤��ѯ
	 * 
	 * @param criteria
	 * @return
	 */
	public List<Certificate> receiveDocument(String searchStr, String areaCode) {
		String paramType = "chinese"; //�����Ĭ�������ѯ����Ϊ ����
		
		try{ //�����ѯ��������ת��Ϊ���֣���ı� parType Ϊ ��ֵ��ѯ
			Long.parseLong(searchStr);
			paramType = "number";
		}catch(Exception e){}
		
		Certificate certificate = new Certificate();
		String hql = " from Certificate c where c.state = '" + Constant.CERTIFICATE_STATE_END + "' and c.areaCode = '"+areaCode+"'";
		if("chinese".equals(paramType)) {
			hql += " and c.userName like '%" + searchStr + "%'";
		}
		if("number".equals(paramType)){
			if(searchStr.length() == 18){ //Ĭ���û�����18λ��ѯ����Ϊ���֤��ѯ
				hql += " and c.applicant.peopleId='" + searchStr+"'";
			}else{ //������ģ����ѯ
				hql += " and c.certificate_no like '%" + searchStr + "%'";
			}
		}
		hql += " order by operateTime desc";
		return getPageList(hql, certificate);
	}

	/**
	 * ��֤��ǰ�׶ε������Ƿ��ܱ���½�ߴ���
	 * @return
	 */
	public Boolean isDealCertificate(String cerid, String flowhql) {
		String hql = "select count(*) from Certificate certificate, " + flowhql
				+ " and certificate.id='" + cerid + "'";
		return getCount(hql) > 0;
	}

	public List<Certificate> listByUser(Certificate criteria, Unit user) {
		String sql = "select * from t_workbench_certificate c where c.state != '" + Constant.CERTIFICATE_STATE_END + "'"
				+ "and c.state != '" + Constant.CERTIFICATE_STATE_SUBMITTED + "'"
				+ "and c.id in (select distinct problem_id from t_workbench_deal where worker_id = '" + user.getId() + "')";
		if (StringUtils.hasText(criteria.getUserName())) {
			sql += " and user_name like '%" + criteria.getUserName() + "%'";
		}
		if (StringUtils.hasText(criteria.getTypeName())) {
			sql += " and type_name like '%" + criteria.getTypeName() + "%'";
		}
		if (StringUtils.hasText(criteria.getAreaCode())) {
			sql += " and area_code like '%" + criteria.getAreaCode() + "%' ";
		}
		sql += " order by c.create_time desc";
		return getPageListBySql(sql, criteria, Certificate.class);
	}

	/**
	 *  ������ԱID��֤��ID��ѯ�����Ƿ�������֤��
	 * @param peopleId
	 * @param type_twoid
	 * @return
	 */
	public List<Certificate> exist(String peopleId, String type_twoid) {
		String hql = " from Certificate c where c.applicant.peopleId='" + peopleId
				+ "' and c.credentialsType.id='" + type_twoid + "'";
		if(GUARANTEE_CREDENTIALSTYPEID.equals(type_twoid)) {
			hql += " and ( c.state = 'z0' or c.state = 'z1') ";
		} else {
			hql += " and c.state != 'z2'";
		}
		return HibernateUtils.currentSession().createQuery(hql).list();
	}
	
	public List<Certificate> getListByPeopleIdAndTwoCerId(String peopleId, String type_twoid) {
		String sql = "select * from t_workbench_certificate c "
				+ " left join t_en_applicant t on t.certificate_id = c.id "
				+ " where t.people_id = '" + peopleId + "'"
				+ " and c.type_id = '" + type_twoid + "'"
				+ " and c.state like 'z%' order by c.operate_time desc";
		return getPageListBySql(sql, new Certificate(), Certificate.class);
	}
	
	/**
	 * �ۺϲ�ѯ
	 * @author xwq
	 */
	public List<Certificate> comprehensiveSearch(Certificate certificate, String parentId, String typeId) {
		//String hql = " from Certificate c where c.state != '" + Constant.CERTIFICATE_STATE_NEW + "'";
		String hql = " from Certificate c where 1=1";
		//����Ϊ֤����Ϣ��ѯ
		if (StringUtils.hasText(certificate.getAreaCode())) {
			hql += " and c.areaCode like '" + certificate.getAreaCode() + "%' ";
		}
		/*if ( StringUtils.hasText(finishTime) ) { // �������
			long[] time = this.getCurrentDayTime(finishTime);
			hql += " and c.state = 'z0' and c.operateTime >= " + time[0] + " and c.operateTime <= " + time[1];
		}*/
		if(StringUtils.hasText(parentId)){ //��֤һ����
			hql += " and c.credentialsType.parentId = '" + parentId +"'";
		}
		if(StringUtils.hasText(typeId)){ //��֤������
			hql += " and c.credentialsType.id = '" + typeId + "'";
		}
		if (StringUtils.hasText(certificate.getCertificate_no())) { //������
			hql += " and c.certificate_no like '%" + certificate.getCertificate_no() + "%'";
		}
		if(certificate.getState() != null && "b0".equals(certificate.getState())) {
			hql += "and c.state != '" + Constant.CERTIFICATE_STATE_END  + "'"
					+ " and c.state != '" + Constant.CERTIFICATE_STATE_SUBMITTED + "'"
					+ " and c.state != '" + Constant.CERTIFICATE_STATE_ABANDON + "'";
		} else if (StringUtils.hasText(certificate.getState())) {
			hql += "and c.state = '" + certificate.getState() + "'";
		}
		// ����Ϊ��Ա��Ϣ��ѯ
		if(StringUtils.hasText(certificate.getApplicant().getName())){ //����
			hql += " and c.applicant.name like '%" + certificate.getApplicant().getName() + "%'";
		}
		if(StringUtils.hasText(certificate.getApplicant().getPeopleId())){ //���֤����
			hql += " and c.applicant.peopleId like '%" + certificate.getApplicant().getPeopleId() + "%'";
		}
		if(StringUtils.hasText(certificate.getApplicant().getContactNumber())){ //��ϵ����
			hql += " and c.applicant.contactNumber like '%" + certificate.getApplicant().getContactNumber() + "%'";
		}
		if( ! StringUtils.hasText(certificate.getOrderBy()) ) {
			hql += " order by c.operateTime desc"; // Ĭ������
	    }
		return getPageList(hql, certificate);
	}
	
	
	/**
	 * ���ҵ�ĳһ�������Ͷ�Ӧ�ĸ����б� 
	 * @param busId ҵ��Id
	 * @param typeId һ����Id
	 * @param subTypeId ��������Id
	 * @return ����path,��ʽ,��С,��ע,������id,�м��Id��������
	 */
	public List findAccessoryList(String busId ,String typeId ,String subTypeId){
		String sql = "select a.path,a.ext,a.sizes,a.remarks,a.id as accessory_id,f.id as fj_id,a.name,f.c_accessory_name"
				+ " from t_accepted_certificate_fj f ,t_com_accessory a where"
				+ " f.accessory_id=a.id and f.certificate_id='" + busId
				+ "' and f.credentials_type_id='" + typeId
				+ "' and f.subcredentials_type_id='" + subTypeId
				+ "' order by a.operate_time";
		return HibernateUtils.currentSession().createSQLQuery(sql).list();
	}
	
	/**
	 * ���ҵ�ȫ���ĸ����б� 
	 * @param busId ҵ��Id
	 * @param typeId һ����Id
	 * @param subTypeId ��������Id
	 * @return ����path,��ʽ,��С,��ע,������id,�м��Id��������, ������(��������)
	 */
	public List findAllAccessoryList(String busId ,String typeId){
		String sql = " select a.path, a.ext, a.sizes, a.remarks, a.id accessory_id, f.id fj_id, a.name, f.c_accessory_name as a_name"
				+ " from t_accepted_certificate_fj f , t_com_accessory a , t_subcredentialstype s "
				+ " where f.accessory_id = a.id and f.subcredentials_type_id = s.id "
				+ " and f.certificate_id = '" + busId + "'"
				+ " and f.credentials_type_id = '" + typeId + "'"
				+ " order by a.id";
		return HibernateUtils.currentSession().createSQLQuery(sql).list();
	}
	
	/**
	 * ���ҵ�ȫ���ĸ����б� 
	 * @param busId ҵ��Id
	 * @return ����path,�м��Id��������
	 */
	public List findAllAccessoryList(String busId){
		String sql = " select a.path, a.ext, s.c_name a_name, a.name ca_name"
				+ " from t_accepted_certificate_fj f , t_com_accessory a, t_subcredentialstype s "
				+ " where f.accessory_id = a.id and f.subcredentials_type_id = s.id "
				+ " and f.certificate_id= '" + busId + "' "
				+ " order by a.id";
		return HibernateUtils.currentSession().createSQLQuery(sql).list();
	}
	
	/**
	 * ɾ����֤����֤��ص���Ϣ
	 * @param map
	 */
	public void deleteCertificate(Rundata map, Certificate certificate, String fileRootPath) {
		Session session = HibernateUtils.currentSession();
		if (certificate != null) {
			Transaction transaction = session.getTransaction();
			transaction.begin();
			Object twoCer = null;
			try {
				// ���߰�������ҵ���ɾ��ҵ�������
				if(certificate.getCredentialsType().getFlowType() == FLOWTYPE_ONLINE && certificate.getCredentialsType().getHasForm() == HAS_FORM_YES) {
					CredentialsModule module = credentialsModuleService.findByIdAndAreaCode(certificate.getCredentialsType().getId(), certificate.getCredentialsType().getAreaCode());
					@SuppressWarnings("unchecked")
					List<Object> list = session.createQuery(" from " + module.getModuleName() + " where certificate.id='"
							+ certificate.getId() + "'").list();
					if (list.size() > 0) {
						twoCer = list.get(0);
						if (twoCer != null) {
							deleteAttachByCer(map, session);
							session.delete(twoCer);
						}
					}
				}
				session.delete(certificate);
				if (StringUtils.hasText(certificate.getLeftImage())) {
					FileUtil.deleteFile(fileRootPath + certificate.getLeftImage());
				}
				if (StringUtils.hasText(certificate.getRightImage())) {
					FileUtil.deleteFile(fileRootPath + certificate.getRightImage());
				}
				// ɾ������
				session.createQuery("Delete from Doc d where d.cerId=?").setString(0, certificate.getId()).executeUpdate();
				String docPath = fileRootPath + "certificate/doc/" + certificate.getId();
				String imagePath =  fileRootPath + "CerDecodeImage/" +  certificate.getId();
				FileUtil.deleteDirAndFiles(docPath);
				FileUtil.deleteDirAndFiles(imagePath);
				transaction.commit();
			} catch (Exception e) {
				transaction.rollback();
				logger.error(e.getMessage());
			}
		}
	}
   
	/**
	 * ɾ��֤����Ӧ�ĸ���-Ӳ�̺����ݿ�
	 * @param map
	 */
	private void deleteAttachByCer(Rundata map,Session session){
		AccessoryService accessoryService = new	AccessoryService();
		CertificateService certificateService = new CertificateService();
		
		String cerId = map.getString("id");
		Certificate certificate=(Certificate) session.get(Certificate.class, cerId);
		String typeId=certificate.getCredentialsType().getId();
		List accessoryList= certificateService.findAllAccessoryList(cerId, typeId);
		for(int i = 0; i < accessoryList.size(); i++){
			Object[] obj=(Object[])accessoryList.get(i);
			String accessoryId=obj[4]==null?"-":obj[4].toString();
			Accessory accessory = accessoryService.findById(map, accessoryId);
			if(accessory != null&&StringUtils.hasText(accessory.getPath())&&!accessory.getPath().startsWith("license_library/")){//����ɾ��֤�տ��еĸ���
				accessoryService.deleteFromDisk(map, accessoryId);
				accessoryService.deleteFromDb(accessoryId);
			}
		}
		session.createQuery("delete from CertificateFJ c where c.certificate_id='"+cerId+"'").executeUpdate();
	}
	
	/**
	 * ���滺���еĸ�����Ϣ�����ݿ�
	 * @param map
	 * @param businessId
	 * @return
	 */
    public List<String> saveCerAttachFromCache(Rundata map, String businessId) {
    	AccessoryService accessoryService = new AccessoryService();
        Unit user = sessionUserUtil.getUserFromSession(map.getHttpServletRequest());
        List<String> list = new ArrayList<String>();
        List<Accessory> cacheList = accessoryService.getAllFromSession(map);
        Session session = HibernateUtils.currentSession();
        session.beginTransaction();
        if(cacheList!=null&&cacheList.size()>0){
            for(Accessory ca : cacheList) {
                ca.setBusinessId(businessId);
                ca.setOperateTime(System.currentTimeMillis());
                ca.setOperator(user);
                session.save(ca);
                list.add(ca.getId());
            }
        }
        session.getTransaction().commit();
        return list;
    }
    
    /**
     * ���渽���м��
     */
	public void SaveCertificateFJ(CertificateFJ certificateFJ) {
		excuteAdd(certificateFJ);
	}
	
    /**
     * ��ȡ��ǰ�û���������½�֤����Ŀ
     * @param certificate
     * @return
     */
    public int newCertificate(Certificate criteria, Unit user, String state){
    	String sql = " select count(*) from t_workbench_certificate c where state ='" + state + "'";
		if (StringUtils.hasText(criteria.getAreaCode())) {
			sql += " and area_code like '%" + criteria.getAreaCode() + "%' ";
		}
		sql +=" and c.id not in(select i.busid from t_flow_instance i)";
		sql += " and operator_id='" + user.getId() + "'";
		return getCountSql(sql);
    }
    
    
    /**
     * ��ȡ��ǰ�û�������Ĵ���֤����Ŀ
     * @param certificate
     * @return
     */
    public int waitCertificate(Certificate certificate){
		String hql = "select count(*) from Certificate c where c.id is not null and c.state !='" + Constant.CERTIFICATE_STATE_NEW 
				+ "' and c.state != '" + Constant.CERTIFICATE_STATE_END 
				+"' and c.state != '" + Constant.CERTIFICATE_STATE_SUBMITTED + "'";
		if (StringUtils.hasText(certificate.getAreaCode())) {
		    hql += " and c.areaCode like '%" + certificate.getAreaCode() + "%'";
		}
		if (StringUtils.hasText(certificate.getOperatorId())) {
		    hql += " and c.operatorId ='" + certificate.getOperatorId() + "'";
		}
		int count1 = getCount(hql);
    	return count1;			//���ش���֤����Ŀ
    }
    
    /**
     * ��ȡ��ǰ�û�������İ��֤����Ŀ
     * @param certificate
     * @return
     */
    public int endCertificate(Certificate certificate){
		String hql = "select count(*) from Certificate c where c.id is not null and c.state ='" + Constant.CERTIFICATE_STATE_END + "'";
		if (StringUtils.hasText(certificate.getAreaCode())) {
		    hql += " and c.areaCode like '%" + certificate.getAreaCode() + "%'";
		}
		if (StringUtils.hasText(certificate.getOperatorId())) {
		    hql += " and c.operatorId ='" + certificate.getOperatorId() + "'";
		}
    	return getCount(hql);
    }
    
    /**
     * ���ݰ�֤����������className ��������id ��ȡ������������Ĵ�֤������
     * @param className
     * @param id
     * @return
     * @author xwq
     */
    public List<Object> getTwoCerByClassNameAndCerId(String className, String id){
    	String hql = "from " + className + " where certificate.applicant.peopleId='" + id + "'";
    	return HibernateUtils.currentSession().createQuery(hql).list();
    }
    
    public Object getTwoCerByClassNameAndCid(String className, String id) {
    	String hql = "from cn.sowell.zhsq.enchiridion.pojo." + className + " where certificate.id = '" + id + "'";
    	return HibernateUtils.currentSession().createQuery(hql).uniqueResult();
    }
	    
    /**
     * ͨ��֤��Id��ȡ֤��
     * @param id
     * @return
     */
    public Certificate findById(String id) {
		return (Certificate) getOne(" from Certificate c where c.id='" + id + "'");
	}
    
    public void update(Certificate criteria){
    	Session session = HibernateUtils.currentSession();
        session.beginTransaction();
        session.update(criteria);
        session.getTransaction().commit();
    }
    
    /**
     * @Description: ���ð�֤������,��Ҫ��������:<br>
     * ��֤����������������ɣ��ֱ��ǵ���14λ��ʱ�����ִ���3λ���ĵ����½�����ͳ��������;<br>
     * ���磺 2016��10��12��10ʱ58��34�봴���ĵ����һ����֤�����ż���20161012105834001���Դ�����
     * @author pyyou 2016��10��12��
     * 
     * @param criteria
     */
    public synchronized void save(Certificate criteria){
    	Session session = HibernateUtils.currentSession();
    	session.beginTransaction();
    	DateFormatUtil dateFormatUtil = DateFormatUtil.getInstance("yyyyMMddHHmmss");
    	String no = dateFormatUtil.toString(System.currentTimeMillis());
    	// ͳ�Ƶ����½�����֤���������λ�����Զ�  + 1
    	String sql = "select IFNULL(max(substr(t.certificate_no, 15,3)), 0)  + 1 from t_workbench_certificate t where "
    			+ " FROM_UNIXTIME(t.create_time/1000, '%Y-%m-%d')"
    			+ "= date_format(now(), '%Y-%m-%d')";
    	Integer count = Double.valueOf(session.createSQLQuery(sql).uniqueResult().toString()).intValue();//�ų�����С�����Ӱ��
    	no += String.valueOf(count).length() < 3 ? String.format("%03d", Integer.valueOf(count)) : count;// �����½�ͳ�������������3λ����0��
    	criteria.setCertificate_no(no);
        session.save(criteria);
        session.getTransaction().commit();
    }
    
	/**
	 * ѡ�е�����������ѯ
	 * 
	 * @param criteria
	 * @param flowsql
	 * @param userid
	 * @return
	 */
	public List<Certificate> selected_list(String[] cerIds) {
		String inIds = "";
		for(String cerId:cerIds){
			inIds += ",'"+cerId+"'";
		}
		if(StringUtils.hasText(inIds)){
			inIds = inIds.substring(1);
		}
		String hql = "from Certificate c where c.id in ( "+inIds+" ) order by c.operateTime desc";
		return (List<Certificate>)getListByHQL(hql);
	}
	/**
	 * ����������ѯ
	 * 
	 * @param criteria
	 * @param flowsql
	 * @param userid
	 * @return
	 */
	public List<Certificate> batchbj_list(Certificate criteria, String flowsql, String userid,String applayItem) {
		// ���ݹ������ҳ����Դ��������
		String hql = "select c.* from t_en_recovery_aid aid left join  (select certificate.* from t_workbench_certificate certificate,"
				+ flowsql;
		hql += " ) c on c.id = aid.c_id  where state != '" + Constant.CERTIFICATE_STATE_END + "' and state != '" + Constant.CERTIFICATE_STATE_SUBMITTED + "'";
		if(StringUtils.hasText(applayItem)){
			hql += " and aid.apply_item like '%,"+applayItem+",%'"; //�����ֵ�����֤��״̬Ϊ c0
		}
		if (StringUtils.hasText(criteria.getUserName())) {
			hql += " and c.user_name like '%" + criteria.getUserName() + "%'";
		}
		if (StringUtils.hasText(criteria.getTypeName())) {
			hql += " and c.type_name like '%" + criteria.getTypeName() + "%'";
		}
		if (StringUtils.hasText(criteria.getAreaCode())) {
			hql += " and c.area_code like '%" + criteria.getAreaCode() + "%' ";
		}
		hql += " order by c.operate_time desc";
		return getPageListBySql(hql, criteria, Certificate.class);
	}
	
	/**
	 * ��������list��ѯ
	 * 
	 * @param criteria
	 * @param flowsql
	 * @param userid
	 * @return
	 */
	public List<Certificate> searchBatchApprovalList(Certificate criteria, String flowsql, String userid, String typeId) {
		// ���ݹ������ҳ����Դ��������
		String hql = "select c.* from (select certificate.* from t_workbench_certificate certificate,"
				+ flowsql;
		hql += " ) c  where c.state != '" + Constant.CERTIFICATE_STATE_END + "' and state != '" + Constant.CERTIFICATE_STATE_SUBMITTED + "' ";
		if(StringUtils.hasText(typeId)){
			hql += " and c.type_id = '" + typeId + "'";
		}else{
			hql += " and c.type_id = 'nothing'";
		}
		/*if(StringUtils.hasText(typeId)){
			hql += " and c.type_id = '" + typeId + "'";
		}else{
			hql += " and c.type_id in ( select cd.id from t_credentialstype cd left join t_credentialstype c on cd.parent_id = c.id "
					+ "where c.c_state = 'Y' and c.parent_id is null and cd.c_state = 'Y' and cd.parent_id is not null "
					+ "and cd.c_batch_acceptance = '1' GROUP BY cd.id ) ";
		}*/
		if (StringUtils.hasText(criteria.getUserName())) {
			hql += " and c.user_name like '%" + criteria.getUserName() + "%'";
		}
		if (StringUtils.hasText(criteria.getAreaCode())) {
			hql += " and c.area_code like '%" + criteria.getAreaCode() + "%' ";
		}
		hql += " order by c.operate_time desc";
		return getPageListBySql(hql, criteria, Certificate.class);
	}
	/**
	 * ��ȡǰ������֤����������ҳ�ã�
	 * 
	 * @param areaCode
	 * @return
	 */
	public List<Certificate> getTopListByAreaCode(String areaCode, int num) {
		Session session = HibernateUtils.currentSession();
		String sql = "select * from t_workbench_certificate t where t.area_code = :areaCode order by t.operate_time desc limit 0,:endNum";
		Query query = session.createSQLQuery(sql).addEntity(Certificate.class);
		query.setString("areaCode", areaCode);
		query.setInteger("endNum", num);
		return query.list();
	}
	/**
	 * �޸ĸ������� �ж������Ƿ��ظ�
	 * @param cerId
	 * @param name
	 * @return
	 */
	public int getCountFJByCerIdAndName(String cerId,String name){
		String sql = " select count(*) from t_accepted_certificate_fj f where f.certificate_id = '"+cerId+"' and f.c_accessory_name ='"+name+"'";
		return getCountSql(sql);
	}
	/**
	 * ͨ�������м�����֤��֤id
	 * @param cerId
	 * @param typeId
	 * @param subTypeId
	 * @return
	 */
	public String findBusIdFromCertificateFj(String cerId ,String typeId ,String subTypeId){
		String sql = "select f.bus_id from t_accepted_certificate_fj f where "
				+ "f.certificate_id='" + cerId + "' and f.accessory_id is null "
				+ "and f.credentials_type_id='" + typeId + "' "
				+ "and f.subcredentials_type_id='" + subTypeId+ "' ";
		List qs = HibernateUtils.currentSession().createSQLQuery(sql).list();
		if(qs!=null && qs.size()>0){
			return qs.get(0).toString();
		}
		return null;
	}
	/**
	 * ɾ����֤��֤��صĸ����м��
	 * @param cerId
	 */
	public void deleteCertificateFJByBusId(String cerId){
		String hql = "delete from CertificateFJ c where c.busId='"+cerId+"'";
		excuteUpdate(hql);
	}
	/**
	 * ɾ����Ϣ ͬʱ����Ϣ��ӵ�t_sync
	 * @param id ɾ����id
	 * @param type ����
	 * @param areaCode �������
	 */
	public void addSyncData(String id,String type,String areaCode){
		String sql = "insert into t_sync_delete_data (id,c_type,c_area_code) values( '"+id+"', '"+type+"', '"+areaCode+"')";
		excuteUpdateSQL(sql);
	}
	public void setCertificateNoToNull(String certificateNo,String areaCode){
		Long nowTime = System.currentTimeMillis();
		String sql = "update t_certificate_online set c_certificate_no = null , update_time = '"+nowTime+"' where c_certificate_no = '"+certificateNo+"' and c_area_code like '"+areaCode+"%' ";
		excuteUpdateSQL(sql);
	}
}