/**
 * PatchInventory.java
 * Uses javassist to patch Inventory.getPaperdollItemDisplayId()
 * to check DressMeManager BEFORE falling through to item.getDisplayId().
 *
 * Compile:  javac -cp javassist.jar PatchInventory.java
 * Run:      java  -cp .:javassist.jar PatchInventory /path/to/GameServer.jar
 */
import javassist.*;
import javassist.bytecode.*;
import java.io.*;

public class PatchInventory
{
    public static void main(String[] args) throws Exception
    {
        if (args.length < 1)
        {
            System.out.println("Usage: java PatchInventory <GameServer.jar>");
            System.exit(1);
        }
        String jarPath = args[0];

        // ── Load the Inventory class from the JAR ─────────────────
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(jarPath);
        pool.insertClassPath(new ClassClassPath(PatchInventory.class));

        CtClass inventory = pool.get("org.l2jmobius.gameserver.model.itemcontainer.Inventory");
        CtClass dressMgr  = pool.get("org.l2jmobius.gameserver.custom.dressme.DressMeManager");

        // ── Find getPaperdollItemDisplayId(int) ───────────────────
        CtMethod method = inventory.getDeclaredMethod(
            "getPaperdollItemDisplayId",
            new CtClass[]{ CtClass.intType }
        );

        System.out.println("[PatchInventory] Original body:");
        System.out.println(method.getMethodInfo().toString());

        // ── Build the new method body ─────────────────────────────
        // Keep transmog check, add DressMe check wrapped in try-catch(Throwable)
        // so any DressMeManager init failure never causes a dark screen.
        String newBody =
            "{"
            + "  org.l2jmobius.gameserver.model.item.instance.Item item = this._paperdoll[$1];"
            + "  if (item != null) {"
            + "    int transmogId;"
            + "    if (org.l2jmobius.gameserver.config.custom.TransmogConfig.ENABLE_TRANSMOG"
            + "        && (transmogId = item.getTransmogId()) > 0) {"
            + "      return transmogId;"
            + "    }"
            + "    try {"
            + "      int dressMeId = org.l2jmobius.gameserver.custom.dressme.DressMeManager"
            + "                         .getInstance().getVisualId(getOwnerId(), $1);"
            + "      if (dressMeId > 0) { return dressMeId; }"
            + "    } catch (Throwable t) {"
            + "      java.util.logging.Logger.getLogger(\"DressMe\").warning("
            + "        \"DressMe visual lookup failed: \" + t.getMessage());"
            + "    }"
            + "    return item.getDisplayId();"
            + "  }"
            + "  return 0;"
            + "}";

        method.setBody(newBody);

        System.out.println("[PatchInventory] Method patched successfully.");

        // ── Write the patched class to a .class file ──────────────
        byte[] bytecode = inventory.toBytecode();
        String outDir = "patched_classes";
        new File(outDir + "/org/l2jmobius/gameserver/model/itemcontainer/").mkdirs();
        String outPath = outDir + "/org/l2jmobius/gameserver/model/itemcontainer/Inventory.class";
        try (FileOutputStream fos = new FileOutputStream(outPath))
        {
            fos.write(bytecode);
        }
        System.out.println("[PatchInventory] Written to: " + outPath);
    }
}
