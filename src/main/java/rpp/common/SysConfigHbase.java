package rpp.common;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * hbase配置信息对像
 */
public class SysConfigHbase{
    private Properties property;
    private static SysConfigHbase sysconfig;

    private SysConfigHbase(String uri) throws Exception {
        init(uri);
    }

    /**
     * @return
     */
    public static SysConfigHbase getInstance(String uri) throws Exception {
        if (sysconfig == null){
            sysconfig = new SysConfigHbase(uri);
        }
        return sysconfig;
    }

    private void init(String uri) throws Exception {

        //FileSystem fs = FileSystem.get(new Configuration());
        Properties Props = new Properties();
        try{
            //Props.load(fs.open(new Path(uri)));
            Props.load(new FileInputStream(uri));
        } catch (Exception e){
            e.printStackTrace();
            throw new Exception("文件读取错误");
        }
        property = Props;
    }

    /**
     *
     * @param name
     * @return
     */
    public static String  getValue(String uri,String name) throws Exception {
        String ret;
        ret =SysConfigHbase.getInstance(uri).property.getProperty(name) ;
        return ret;
    }

}