# BlueUtils
经典蓝牙搜索，连接，数据传输小DEMO

通过经典模式 搜索 蓝牙应用。蓝牙有蓝牙1.0、蓝牙2.0、蓝牙3.0、蓝牙4.0之类的以数字结尾的蓝牙版本号，而实际上，在最新
的标准中，已经不再使用数字版本号作为蓝牙版本的区分了，取而代之的是经典蓝牙与低功耗蓝牙（BLE）这两种区别。BLE 蓝牙不
做过多讲解。具体的信息大家可以参考。
http://www.loverobots.cn/the-analysis-is-simple-compared-with-the-classic-bluetooth-and-bluetooth-low-energy-in-android.html

# 流程
  发现设备->配对/绑定设备->建立连接->数据通信
  经典蓝牙和低功耗蓝牙除了配对/绑定这个环节是一样的之外，其它三个环节都是不同的。
  
# 详解
  公司最近在要做一个蓝牙与串口通讯的项目，然后就涉及到手机端与蓝牙的连接及数据交互。大致需求就是通过手机搜索硬件蓝牙
  设备，然后连接上蓝牙，通过手机端的指令消息来获取串口信息，在通过蓝牙返回数据到手机端。在这之前看了一些开源的项目，
  包括BluetoothKit，FastBle，BluetoothHelper等其中BluetoothKit和FastBle只支持BLE 模式蓝牙，因为硬件的模式是
  经典模式，后来自己在两个项目的基础上做了一些修改，然后可以搜索到经典蓝牙。但是怎么也是连接不上我们的硬件设备。（应
  该是底层不是经典蓝牙连接导致。）后来发现了BluetoothHelper项目。在这个项目的基础上做了一些修改及优化 ，能够满足
  项目需求，现在将这个项目做了分包及优化。然后在这分享自己的一些踩坑心得。



  在页面首先初始化一个BlueManager

  private BlueManager bluemanage;

  bluemanage = BlueManager.from(MainActivity.this);

  然后通过 调用 searchDevices 获取蓝牙设备，有些手机搜索开始之后 一直不走onSearchCompleted。

    bluemanage.searchDevices(new OnSearchDeviceListener() {
                    @Override
                    public void onStartDiscovery() {
                        Log.d(TAG, "onStartDiscovery()");
                    }

                    @Override
                    public void onNewDeviceFound(BluetoothDevice device) {
                        Log.d(TAG, "new device: " + device.getName() + " " + device.getAddress());
                    }

                    @Override
                    public void onSearchCompleted(List<BluetoothDevice> bondedList, List<BluetoothDevice> newList) {
                        Log.d(TAG, "SearchCompleted: bondedList" + bondedList.toString());
                        Log.d(TAG, "SearchCompleted: newList" + newList.toString());
                    }

                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                    }
                });

   通过 BlueManager里的searchDevices方法，里边其实就是获取了一个BluetoothAdapter然后，通过调用mBluetoothAda
   pter.startDiscovery();来搜索经典蓝牙设备。这里如果调用 mBluetoothAdapter.startLeScan(mLeScanCallback);
   搜索的就是BLE蓝牙。然后在这之前需要动态注册一个BroadcastReceiver来监听 蓝牙的搜索情况，在通过onReceive中去判
   断设备的类型，是不是新设备，是不是已经链接过。搜索完成同样也会被监听到。

   搜索代码如下

      public void searchDevices(OnSearchDeviceListener listener) {

            checkNotNull(listener);
            if (mBondedList == null) mBondedList = new ArrayList<>();
            if (mNewList == null) mNewList = new ArrayList<>();

            mOnSearchDeviceListener = listener;

            if (mBluetoothAdapter == null) {
                mOnSearchDeviceListener.onError(new NullPointerException(DEVICE_HAS_NOT_BLUETOOTH_MODULE));
                return;
            }

            if (mReceiver == null) mReceiver = new Receiver();//注册receiver监听回调

            // ACTION_FOUND
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            mContext.registerReceiver(mReceiver, filter);

            // ACTION_DISCOVERY_FINISHED
            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            mContext.registerReceiver(mReceiver, filter);

            mNeed2unRegister = true;

            mBondedList.clear();
            mNewList.clear();

            if (mBluetoothAdapter.isDiscovering())    //先判断是否在扫描
                mBluetoothAdapter.cancelDiscovery();  //取消扫描
            mBluetoothAdapter.startDiscovery();       //开始扫描蓝牙

            if (mOnSearchDeviceListener != null)
                mOnSearchDeviceListener.onStartDiscovery();

        }

   到这里搜索的大概流程就是走完了。接下来说下配对连接。

      bluemanage.connectDevice("00:21:13:02:9B:F1", new OnConnectListener() {
                         @Override
                         public void onConnectStart() {
                             Log.i("blue", "onConnectStart");
                         }

                         @Override
                         public void onConnectting() {
                             Log.i("blue", "onConnectting");
                         }

                         @Override
                         public void onConnectFailed() {
                             Log.i("blue", "onConnectFailed");
                         }

                         @Override
                         public void onConectSuccess() {
                             Log.i("blue", "onConectSuccess");
                         }

                         @Override
                         public void onError(Exception e) {
                             Log.i("blue", "onError");
                         }
                     });

    就是开启一个线程去连接远程蓝牙


        public void connectDevice(String mac, OnConnectListener listener) {
            if (mCurrStatus != STATUS.CONNECTED) {
                if (mac == null || TextUtils.isEmpty(mac))
                    throw new IllegalArgumentException("mac address is null or empty!");
                if (!BluetoothAdapter.checkBluetoothAddress(mac))
                    throw new IllegalArgumentException("mac address is not correct! make sure it's upper case!");
                ConnectDeviceRunnable connectDeviceRunnable = new ConnectDeviceRunnable(mac, listener);
                checkNotNull(mExecutorService);
                mExecutorService.submit(connectDeviceRunnable);
            } else {
                Log.i("blue", "the blue is connected !");
            }
        }

   在连接的线程run方法中，通过调用mBluetoothAdapter.getRemoteDevice 获取远程蓝牙信息，通过createInsecureRfcommSocketToServiceRecord
   获得一个与远程蓝牙的socket连接。通过这个进行连接及数据的读写。

         BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(mac);
                   mBluetoothAdapter.cancelDiscovery();
                   mCurrStatus = STATUS.FREE;
                   try {
                       Log.d(TAG, "prepare to connect: " + remoteDevice.getAddress() + " " + remoteDevice.getName());
                       mSocket = remoteDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(Constants.STR_UUID));
                       listener.onConnectting();
                       mSocket.connect();
                       mInputStream = mSocket.getInputStream();
                       mOutputStream = mSocket.getOutputStream();
                       mCurrStatus = STATUS.CONNECTED;
                       if (listener != null) {
                           listener.onConectSuccess();
                       }
                   } catch (Exception e) {
                       e.printStackTrace();
                       if (listener != null)
                           listener.onConnectFailed();
                       try {
                           mInputStream.close();
                           mOutputStream.close();
                       } catch (IOException closeException) {
                           closeException.printStackTrace();
                       }
                       mCurrStatus = STATUS.FREE;
                   }




