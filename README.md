## 野火对象存储网关
野火专业版IM服务只能支持少数几种对象存储服务，为了满足客户接入自有对象存储服务的需要，可以启用野火对象存储网关。客户端上传文件时上传到网关，网关把文件上传到客户自由对象存储。下载文件时直接从客户对象存储服务下载。

## 配置
在IM服务中配置```media.server.media_type```为```4```。配置```media.server_host```为网关的地址，```media.server_port```为网关的端口。

## 对接
检查```UploadFileAction```对象，在此对象中有3个方法如下:
```
public boolean beforeData();
public void onData(long pos, byte[] data, int length);
public void afterData();
```
在这3个方法中上传到客户自有对象存储服务即可。

## 编译
在安装JDK1.8以上及maven的前提下，在命令行中执行```mvn clean package```，生成的目标文件在```./distribution/target/distribution-xxxx-bundle-tar.tar.gz```
> 由于使用了一个git的maven插件，如果本地没有git信息就会编译出错，请使用```git clone```的方法下载代码，或者下载压缩包解压后在根目录创建```.git```的空目录。建议用```git clone```的方式下载代码。

## 运行
解压编译后的压缩包，修改config目录下的配置，注意配置要与IM服务的内容保持一致。在bin目录下执行
```
sh ./wildfirehcat.sh
```
