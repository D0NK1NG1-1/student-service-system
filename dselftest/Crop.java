import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** 临时工具: 裁剪并放大截图的指定区域, 用于放大检查细节 */
public class Crop {
    public static void main(String[] a) throws Exception {
        // 参数: 源文件 输出文件 x y w h 缩放倍数
        BufferedImage src = ImageIO.read(new File(a[0]));
        int x = Integer.parseInt(a[2]), y = Integer.parseInt(a[3]);
        int w = Integer.parseInt(a[4]), h = Integer.parseInt(a[5]);
        int scale = Integer.parseInt(a[6]);
        BufferedImage out = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < h * scale; j++) {
            for (int i = 0; i < w * scale; i++) {
                out.setRGB(i, j, src.getRGB(x + i / scale, y + j / scale));
            }
        }
        ImageIO.write(out, "png", new File(a[1]));
        System.out.println("cropped -> " + a[1] + " " + (w * scale) + "x" + (h * scale));
    }
}
