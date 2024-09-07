
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 塔台指令
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/zhiling")
public class ZhilingController {
    private static final Logger logger = LoggerFactory.getLogger(ZhilingController.class);

    private static final String TABLE_NAME = "zhiling";

    @Autowired
    private ZhilingService zhilingService;


    @Autowired
    private TokenService tokenService;

    @Autowired
    private DictionaryService dictionaryService;//字典
    @Autowired
    private GonggaoService gonggaoService;//公告信息
    @Autowired
    private HangbanService hangbanService;//航班信息
    @Autowired
    private HangbanYuyueService hangbanYuyueService;//起飞降落请求
    @Autowired
    private YonghuService yonghuService;//用户
    @Autowired
    private UsersService usersService;//管理员


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        CommonUtil.checkMap(params);
        PageUtils page = zhilingService.queryPage(params);

        //字典表数据转换
        List<ZhilingView> list =(List<ZhilingView>)page.getList();
        for(ZhilingView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        ZhilingEntity zhiling = zhilingService.selectById(id);
        if(zhiling !=null){
            //entity转view
            ZhilingView view = new ZhilingView();
            BeanUtils.copyProperties( zhiling , view );//把实体数据重构到view中
            //级联表 航班信息
            //级联表
            HangbanEntity hangban = hangbanService.selectById(zhiling.getHangbanId());
            if(hangban != null){
            BeanUtils.copyProperties( hangban , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "username", "password", "newMoney", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setHangbanId(hangban.getId());
            }
            //级联表 用户
            //级联表
            YonghuEntity yonghu = yonghuService.selectById(zhiling.getYonghuId());
            if(yonghu != null){
            BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "username", "password", "newMoney", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setYonghuId(yonghu.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody ZhilingEntity zhiling, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,zhiling:{}",this.getClass().getName(),zhiling.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("用户".equals(role))
            zhiling.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<ZhilingEntity> queryWrapper = new EntityWrapper<ZhilingEntity>()
            .eq("hangban_id", zhiling.getHangbanId())
            .eq("yonghu_id", zhiling.getYonghuId())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ZhilingEntity zhilingEntity = zhilingService.selectOne(queryWrapper);
//        if(zhilingEntity==null){
            zhiling.setInsertTime(new Date());
            zhiling.setCreateTime(new Date());
            zhilingService.insert(zhiling);
            return R.ok();
//        }else {
//            return R.error(511,"表中有相同数据");
//        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody ZhilingEntity zhiling, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,zhiling:{}",this.getClass().getName(),zhiling.toString());
        ZhilingEntity oldZhilingEntity = zhilingService.selectById(zhiling.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("用户".equals(role))
//            zhiling.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        if("".equals(zhiling.getZhilingText()) || "null".equals(zhiling.getZhilingText())){
                zhiling.setZhilingText(null);
        }
        if("".equals(zhiling.getReplyText()) || "null".equals(zhiling.getReplyText())){
                zhiling.setReplyText(null);
        }
        zhiling.setUpdateTime(new Date());

            zhilingService.updateById(zhiling);//根据id更新
            return R.ok();
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<ZhilingEntity> oldZhilingList =zhilingService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        zhilingService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //.eq("time", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
        try {
            List<ZhilingEntity> zhilingList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            ZhilingEntity zhilingEntity = new ZhilingEntity();
//                            zhilingEntity.setHangbanId(Integer.valueOf(data.get(0)));   //航班 要改的
//                            zhilingEntity.setYonghuId(Integer.valueOf(data.get(0)));   //用户 要改的
//                            zhilingEntity.setZhilingText(data.get(0));                    //指令内容 要改的
//                            zhilingEntity.setInsertTime(date);//时间
//                            zhilingEntity.setReplyText(data.get(0));                    //回复内容 要改的
//                            zhilingEntity.setUpdateTime(sdf.parse(data.get(0)));          //回复时间 要改的
//                            zhilingEntity.setCreateTime(date);//时间
                            zhilingList.add(zhilingEntity);


                            //把要查询是否重复的字段放入map中
                        }

                        //查询是否重复
                        zhilingService.insertBatch(zhilingList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }




    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = zhilingService.queryPage(params);

        //字典表数据转换
        List<ZhilingView> list =(List<ZhilingView>)page.getList();
        for(ZhilingView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Integer id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        ZhilingEntity zhiling = zhilingService.selectById(id);
            if(zhiling !=null){


                //entity转view
                ZhilingView view = new ZhilingView();
                BeanUtils.copyProperties( zhiling , view );//把实体数据重构到view中

                //级联表
                    HangbanEntity hangban = hangbanService.selectById(zhiling.getHangbanId());
                if(hangban != null){
                    BeanUtils.copyProperties( hangban , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "username", "password", "newMoney", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setHangbanId(hangban.getId());
                }
                //级联表
                    YonghuEntity yonghu = yonghuService.selectById(zhiling.getYonghuId());
                if(yonghu != null){
                    BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "username", "password", "newMoney", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYonghuId(yonghu.getId());
                }
                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody ZhilingEntity zhiling, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,zhiling:{}",this.getClass().getName(),zhiling.toString());
        Wrapper<ZhilingEntity> queryWrapper = new EntityWrapper<ZhilingEntity>()
            .eq("hangban_id", zhiling.getHangbanId())
            .eq("yonghu_id", zhiling.getYonghuId())
            .eq("zhiling_text", zhiling.getZhilingText())
            .eq("reply_text", zhiling.getReplyText())
//            .notIn("zhiling_types", new Integer[]{102})
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ZhilingEntity zhilingEntity = zhilingService.selectOne(queryWrapper);
        if(zhilingEntity==null){
            zhiling.setInsertTime(new Date());
            zhiling.setCreateTime(new Date());
        zhilingService.insert(zhiling);

            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

}

