/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.sandbox;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.core.Stream2BufferedImage;
import org.im4java.process.ProcessStarter;

public class Test {

    public static void main(String[] args) throws Exception {
        String myPath = "f:\\temp\\im";
        ProcessStarter.setGlobalSearchPath(myPath);

        IMOperation op = new IMOperation();
        op.addImage();                        // input
        op.addImage("cr2:-");

        ConvertCmd convert = new ConvertCmd();
        Stream2BufferedImage s2b = new Stream2BufferedImage();
        convert.setOutputConsumer(s2b);
        String[] images = {"f:/temp/IMG_3058.jpg"};
        // run command and extract BufferedImage from OutputConsumer
        convert.run(op,(Object[]) images);
        BufferedImage img = s2b.getImage();
        System.out.println(img);

        long time = System.currentTimeMillis();
        op = new IMOperation();
        op.addImage();                        // input
        op.addImage("cr2:-");

        convert = new ConvertCmd();
        s2b = new Stream2BufferedImage();
        convert.setOutputConsumer(s2b);
        // run command and extract BufferedImage from OutputConsumer
        convert.run(op,(Object[]) images);
        img = s2b.getImage();
        long time2 = System.currentTimeMillis();

        img = ImageIO.read(new BufferedInputStream(Files.newInputStream(new File("f:/IMG_2878.jpg").toPath())));

        long time3 = System.currentTimeMillis();
        img = ImageIO.read(new BufferedInputStream(Files.newInputStream(new File("f:/IMG_2878.jpg").toPath())));
        long time4 = System.currentTimeMillis();

        System.out.println(time2 - time);
        System.out.println(time4 - time3);

        // File file = new File("F:/IMG_5165.CR2");//f:/IMG_2878.jpg");
        // EXIFData data = ImageUtil.readExifData(file);
        //// long time = System.currentTimeMillis();
        //// for (int i = 0; i <3000; i++) {
        //// EXIFData data = ImageUtil.readExifData(file);
        //// }
        ////
        //// long timet = System.currentTimeMillis();
        //// System.out.println(timet - time);
        // if (true) return;
        // File f = new File("f:/IMG_2878.jpg");
        // ImageIO.setUseCache(false);
        //
        // FileInputStream fis = new FileInputStream(f);
        // final BufferedImage image2 = ImageIO.read(fis);
        //
        // fis.close();
        // long time3 = System.currentTimeMillis();
        // fis = new FileInputStream(f);
        // final BufferedImage image = ImageIO.read(fis);
        //
        // long time4 = System.currentTimeMillis();
        // System.out.println(time4 - time3);
        // System.out.println(image);

        // String myPath = "C:\\Program Files\\GraphicsMagick-1.3.25-Q8";
        // ProcessStarter.setGlobalSearchPath(myPath);
        //
        // GraphicsMagickCmd cmd = new GraphicsMagickCmd("convert");
        // cmd.setSearchPath(myPath);
        //
        // ProcessExecutor exec = new ProcessExecutor();
        //
        //
        // File zda = new File("F:/Izbor");
        // long time = System.currentTimeMillis();
        // IMOperation op = new IMOperation();
        // op.addImage(1);
        // op.thumbnail(null,Integer.valueOf(1200));
        // op.addImage(1);
        // int i = 0;
        // for (File f : zda.listFiles()) {
        // if (!f.getName().endsWith(".jpg")) continue;
        // // op.addImage(f.getAbsolutePath());
        // // cmd.run(op,f.getAbsolutePath(), "F:/Izbor2/th" + i + ".jpg");
        //
        // // ConvertCmd cmd = new ConvertCmd();
        // ProcessTask pt = cmd.getProcessTask(op,f.getAbsolutePath(),"F:/Izbor2/th" + i + ".jpg");
        // exec.execute(pt);
        //
        // System.out.println(f);
        // i++;
        // }
        //
        // exec.shutdown();
        //
        // exec.awaitTermination(60L,TimeUnit.SECONDS);
        // long time2 = System.currentTimeMillis();
        // System.out.println(time2 - time);
    }
}
