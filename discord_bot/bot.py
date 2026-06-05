import discord
from discord.ext import commands, tasks
from discord import app_commands
import aiomysql
import asyncio
import os
import io
import aiohttp
from dotenv import load_dotenv
from datetime import datetime, timezone
import random

load_dotenv()

DB_HOST = os.getenv("DB_HOST", "127.0.0.1")
DB_PORT = int(os.getenv("DB_PORT", 3306))
DB_USER = os.getenv("DB_USER", "root")
DB_PASS = os.getenv("DB_PASS", "")
DB_NAME = os.getenv("DB_NAME", "l2jdb")

GAME_HOST = os.getenv("GAME_HOST", "127.0.0.1")
GAME_PORT = int(os.getenv("GAME_PORT", 7777))

TOKEN = os.getenv("DISCORD_TOKEN")
GUILD_ID = int(os.getenv("GUILD_ID", 0))

CLASS_NAMES = {
    0: "Human Fighter", 1: "Warrior", 2: "Gladiator", 3: "Warlord",
    4: "Human Knight", 5: "Paladin", 6: "Dark Avenger", 7: "Rogue",
    8: "Treasure Hunter", 9: "Hawkeye", 10: "Human Mystic", 11: "Human Wizard",
    12: "Sorcerer", 13: "Necromancer", 14: "Warlock", 15: "Cleric",
    16: "Bishop", 17: "Prophet", 18: "Elven Fighter", 19: "Elven Knight",
    20: "Temple Knight", 21: "Swordsinger", 22: "Elven Scout", 23: "Plainswalker",
    24: "Silver Ranger", 25: "Elven Mystic", 26: "Elven Wizard", 27: "Spellsinger",
    28: "Elemental Summoner", 29: "Elven Oracle", 30: "Elven Elder",
    31: "Dark Fighter", 32: "Palus Knight", 33: "Shillien Knight", 34: "Bladedancer",
    35: "Assassin", 36: "Abyss Walker", 37: "Phantom Ranger", 38: "Dark Elven Mystic",
    39: "Dark Wizard", 40: "Spellhowler", 41: "Phantom Summoner", 42: "Shillien Oracle",
    43: "Shillien Elder", 44: "Orc Fighter", 45: "Orc Raider", 46: "Destroyer",
    47: "Monk", 48: "Tyrant", 49: "Orc Mystic", 50: "Orc Shaman", 51: "Overlord",
    52: "Warcryer", 53: "Dwarven Fighter", 54: "Scavenger", 55: "Bounty Hunter",
    56: "Artisan", 57: "Warsmith", 88: "Duelist", 89: "Dreadnought",
    90: "Phoenix Knight", 91: "Hell Knight", 92: "Sagittarius", 93: "Adventurer",
    94: "Archmage", 95: "Soultaker", 96: "Arcana Lord", 97: "Cardinal",
    98: "Hierophant", 99: "Eva's Templar", 100: "Sword Muse", 101: "Wind Rider",
    102: "Moonlight Sentinel", 103: "Mystic Muse", 104: "Elemental Master",
    105: "Eva's Saint", 106: "Shillien Templar", 107: "Spectral Dancer",
    108: "Ghost Hunter", 109: "Ghost Sentinel", 110: "Storm Screamer",
    111: "Spectral Master", 112: "Shillien Saint", 113: "Titan", 114: "Grand Khavatari",
    115: "Dominator", 116: "Doomcryer", 117: "Fortune Seeker", 118: "Maestro",
    123: "Doombringer", 124: "Soul Hound (M)", 125: "Soul Hound (F)",
    126: "Trickster", 127: "Inspector", 128: "Judicator",
}

intents = discord.Intents.default()
intents.message_content = True
intents.members = True

bot = commands.Bot(command_prefix="!", intents=intents)
db_pool: aiomysql.Pool | None = None


async def get_pool() -> aiomysql.Pool:
    global db_pool
    if db_pool is None or db_pool._closed:
        db_pool = await aiomysql.create_pool(
            host=DB_HOST, port=DB_PORT,
            user=DB_USER, password=DB_PASS,
            db=DB_NAME, autocommit=True, minsize=1, maxsize=5,
        )
    return db_pool


async def query(sql: str, args=None) -> list[dict]:
    pool = await get_pool()
    async with pool.acquire() as conn:
        async with conn.cursor(aiomysql.DictCursor) as cur:
            await cur.execute(sql, args)
            return await cur.fetchall()


async def execute(sql: str, args=None):
    pool = await get_pool()
    async with pool.acquire() as conn:
        async with conn.cursor() as cur:
            await cur.execute(sql, args)
            return cur.lastrowid


async def check_server_online() -> bool:
    try:
        reader, writer = await asyncio.wait_for(
            asyncio.open_connection(GAME_HOST, GAME_PORT), timeout=3
        )
        writer.close()
        await writer.wait_closed()
        return True
    except Exception:
        return False


# ── Slash commands ────────────────────────────────────────────────────────────

@bot.tree.command(name="status", description="Estado del servidor de juego")
async def cmd_status(interaction: discord.Interaction):
    await interaction.response.defer()
    online = await check_server_online()
    if online:
        rows = await query(
            "SELECT COUNT(*) AS cnt FROM characters WHERE online = 1"
        )
        players = rows[0]["cnt"] if rows else 0
        embed = discord.Embed(
            title="🟢 Servidor ONLINE",
            description=f"**Jugadores conectados:** {players}",
            color=discord.Color.green(),
        )
    else:
        embed = discord.Embed(
            title="🔴 Servidor OFFLINE",
            color=discord.Color.red(),
        )
    embed.set_footer(text=f"Lineage 2 H5 • {datetime.now().strftime('%d/%m/%Y %H:%M')}")
    await interaction.followup.send(embed=embed)


@bot.tree.command(name="toppvp", description="Top 10 jugadores por PvP kills")
async def cmd_toppvp(interaction: discord.Interaction):
    await interaction.response.defer()
    rows = await query(
        "SELECT char_name, pvpkills, pkkills, classid "
        "FROM characters WHERE pvpkills > 0 "
        "ORDER BY pvpkills DESC LIMIT 10"
    )
    if not rows:
        await interaction.followup.send("No hay datos de PvP aún.")
        return
    embed = discord.Embed(title="⚔️ Top 10 PvP", color=discord.Color.red())
    lines = []
    medals = ["🥇", "🥈", "🥉"]
    for i, r in enumerate(rows):
        medal = medals[i] if i < 3 else f"`{i+1}.`"
        cls = CLASS_NAMES.get(r["classid"], f"Class {r['classid']}")
        lines.append(
            f"{medal} **{r['char_name']}** — {r['pvpkills']} PvP / {r['pkkills']} PK\n"
            f"   └ {cls}"
        )
    embed.description = "\n".join(lines)
    embed.set_footer(text="Lineage 2 H5")
    await interaction.followup.send(embed=embed)


@bot.tree.command(name="toppk", description="Top 10 jugadores por PK kills")
async def cmd_toppk(interaction: discord.Interaction):
    await interaction.response.defer()
    rows = await query(
        "SELECT char_name, pvpkills, pkkills, classid "
        "FROM characters WHERE pkkills > 0 "
        "ORDER BY pkkills DESC LIMIT 10"
    )
    if not rows:
        await interaction.followup.send("No hay datos de PK aún.")
        return
    embed = discord.Embed(title="💀 Top 10 PK", color=discord.Color.dark_red())
    lines = []
    medals = ["🥇", "🥈", "🥉"]
    for i, r in enumerate(rows):
        medal = medals[i] if i < 3 else f"`{i+1}.`"
        cls = CLASS_NAMES.get(r["classid"], f"Class {r['classid']}")
        lines.append(
            f"{medal} **{r['char_name']}** — {r['pkkills']} PK / {r['pvpkills']} PvP\n"
            f"   └ {cls}"
        )
    embed.description = "\n".join(lines)
    embed.set_footer(text="Lineage 2 H5")
    await interaction.followup.send(embed=embed)


@bot.tree.command(name="topclan", description="Top 10 clanes por reputación")
async def cmd_topclan(interaction: discord.Interaction):
    await interaction.response.defer()
    rows = await query(
        "SELECT clan_name, reputation_score, ally_name, "
        "(SELECT COUNT(*) FROM characters c WHERE c.clanid = cd.clan_id) AS members "
        "FROM clan_data cd "
        "ORDER BY reputation_score DESC LIMIT 10"
    )
    if not rows:
        await interaction.followup.send("No hay datos de clanes.")
        return
    embed = discord.Embed(title="🏰 Top 10 Clanes", color=discord.Color.gold())
    lines = []
    medals = ["🥇", "🥈", "🥉"]
    for i, r in enumerate(rows):
        medal = medals[i] if i < 3 else f"`{i+1}.`"
        ally = f" [{r['ally_name']}]" if r.get("ally_name") else ""
        lines.append(
            f"{medal} **{r['clan_name']}**{ally}\n"
            f"   └ Rep: {r['reputation_score']:,} | Miembros: {r['members']}"
        )
    embed.description = "\n".join(lines)
    embed.set_footer(text="Lineage 2 H5")
    await interaction.followup.send(embed=embed)


@bot.tree.command(name="olympiad", description="Estado y top de las Olimpiadas")
async def cmd_olympiad(interaction: discord.Interaction):
    await interaction.response.defer()
    rows = await query(
        "SELECT c.char_name, on2.olympiad_points, on2.competitions_done, "
        "on2.competitions_won, on2.competitions_lost, on2.class_id "
        "FROM olympiad_nobles on2 "
        "JOIN characters c ON c.charId = on2.charId "
        "ORDER BY on2.olympiad_points DESC LIMIT 10"
    )
    if not rows:
        await interaction.followup.send("No hay datos de Olimpiadas.")
        return
    embed = discord.Embed(
        title="🏆 Top 10 Olimpiadas",
        color=discord.Color.blue(),
    )
    lines = []
    medals = ["🥇", "🥈", "🥉"]
    for i, r in enumerate(rows):
        medal = medals[i] if i < 3 else f"`{i+1}.`"
        cls = CLASS_NAMES.get(r["class_id"], f"Class {r['class_id']}")
        wr = (
            round(r["competitions_won"] / r["competitions_done"] * 100)
            if r["competitions_done"] > 0
            else 0
        )
        lines.append(
            f"{medal} **{r['char_name']}** — {r['olympiad_points']} pts\n"
            f"   └ {cls} | {r['competitions_won']}W/{r['competitions_lost']}L ({wr}%)"
        )
    embed.description = "\n".join(lines)
    embed.set_footer(text="Lineage 2 H5 • Las Olimpiadas finalizan al cierre de período")
    await interaction.followup.send(embed=embed)


@bot.tree.command(name="heroes", description="Lista de héroes actuales")
async def cmd_heroes(interaction: discord.Interaction):
    await interaction.response.defer()
    rows = await query(
        "SELECT c.char_name, h.class_id, h.count "
        "FROM heroes h JOIN characters c ON c.charId = h.charId "
        "WHERE h.played = 1"
    )
    if not rows:
        await interaction.followup.send("No hay héroes activos en este momento.")
        return
    embed = discord.Embed(title="✨ Héroes Actuales", color=discord.Color.purple())
    lines = []
    for r in rows:
        cls = CLASS_NAMES.get(r["class_id"], f"Class {r['class_id']}")
        lines.append(f"⭐ **{r['char_name']}** — {cls} (veces: {r['count']})")
    embed.description = "\n".join(lines)
    embed.set_footer(text="Lineage 2 H5")
    await interaction.followup.send(embed=embed)


@bot.tree.command(name="votos", description="Estado actual de votos del servidor")
async def cmd_votos(interaction: discord.Interaction):
    await interaction.response.defer()
    rows = await query("SELECT voteSite, lastRewardVotes FROM vote_system_global")
    site_names = {0: "HopZone", 1: "L2TopZone", 2: "L2Network", 3: "L2Votes"}
    embed = discord.Embed(
        title="🗳️ Sistema de Votos",
        description="Vota para ganar recompensas en el juego!",
        color=discord.Color.teal(),
    )
    for r in rows:
        name = site_names.get(r["voteSite"], f"Sitio {r['voteSite']}")
        embed.add_field(
            name=name,
            value=f"Votos acumulados: **{r['lastRewardVotes']}**",
            inline=True,
        )
    embed.set_footer(text="¡Vota y ayuda al servidor a crecer!")
    await interaction.followup.send(embed=embed)


@bot.tree.command(name="buscar", description="Buscar información de un personaje")
@app_commands.describe(nombre="Nombre del personaje")
async def cmd_buscar(interaction: discord.Interaction, nombre: str):
    await interaction.response.defer()
    rows = await query(
        "SELECT char_name, level, classid, pvpkills, pkkills, karma, fame, "
        "online, clanid "
        "FROM characters WHERE char_name = %s LIMIT 1",
        (nombre,),
    )
    if not rows:
        await interaction.followup.send(f"Personaje **{nombre}** no encontrado.")
        return
    r = rows[0]
    clan_name = "Sin clan"
    if r["clanid"]:
        clan_rows = await query(
            "SELECT clan_name FROM clan_data WHERE clan_id = %s", (r["clanid"],)
        )
        if clan_rows:
            clan_name = clan_rows[0]["clan_name"]
    status = "🟢 Online" if r["online"] == 1 else "⚫ Offline"
    cls = CLASS_NAMES.get(r["classid"], f"Class {r['classid']}")
    embed = discord.Embed(
        title=f"👤 {r['char_name']}",
        color=discord.Color.blurple(),
    )
    embed.add_field(name="Clase", value=cls, inline=True)
    embed.add_field(name="Nivel", value=r["level"], inline=True)
    embed.add_field(name="Estado", value=status, inline=True)
    embed.add_field(name="PvP", value=r["pvpkills"], inline=True)
    embed.add_field(name="PK", value=r["pkkills"], inline=True)
    embed.add_field(name="Karma", value=r["karma"], inline=True)
    embed.add_field(name="Fama", value=r["fame"], inline=True)
    embed.add_field(name="Clan", value=clan_name, inline=True)
    embed.set_footer(text="Lineage 2 H5")
    await interaction.followup.send(embed=embed)


@bot.tree.command(name="ayuda", description="Lista de todos los comandos disponibles")
async def cmd_ayuda(interaction: discord.Interaction):
    embed = discord.Embed(
        title="📖 Comandos del Bot",
        color=discord.Color.orange(),
    )
    cmds = [
        ("/status", "Estado del servidor y jugadores online"),
        ("/toppvp", "Top 10 jugadores por PvP kills"),
        ("/toppk", "Top 10 jugadores por PK kills"),
        ("/topclan", "Top 10 clanes por reputación"),
        ("/olympiad", "Top 10 de las Olimpiadas"),
        ("/heroes", "Héroes activos del período"),
        ("/votos", "Estado del sistema de votos"),
        ("/buscar <nombre>", "Info de un personaje específico"),
        ("/setup", "Crear canales y categorías del servidor (solo admins)"),
    ]
    for name, desc in cmds:
        embed.add_field(name=name, value=desc, inline=False)
    embed.set_footer(text="Lineage 2 H5 • Bot oficial del servidor")
    await interaction.response.send_message(embed=embed)


@bot.tree.command(name="limpiaremojis", description="Elimina todos los emojis del servidor (solo admins)")
@app_commands.checks.has_permissions(administrator=True)
async def cmd_limpiaremojis(interaction: discord.Interaction):
    await interaction.response.defer(ephemeral=True)
    guild = interaction.guild
    deleted = 0
    failed = 0
    for emoji in list(guild.emojis):
        try:
            await emoji.delete()
            deleted += 1
            await asyncio.sleep(0.3)
        except Exception:
            failed += 1
    embed = discord.Embed(title="🗑️ Emojis eliminados", color=discord.Color.red())
    embed.add_field(name="Eliminados", value=str(deleted), inline=True)
    embed.add_field(name="Fallidos", value=str(failed), inline=True)
    await interaction.followup.send(embed=embed, ephemeral=True)


@bot.tree.command(name="setup", description="Crea los canales y categorías del servidor L2 (solo admins)")
@app_commands.checks.has_permissions(administrator=True)
async def cmd_setup(interaction: discord.Interaction):
    await interaction.response.defer(ephemeral=True)
    guild = interaction.guild

    structure = [
        ("📢 INFORMACIÓN", [
            ("📣│anuncios", "text"),
            ("📋│reglas", "text"),
            ("🗺️│como-conectarse", "text"),
            ("🔔│novedades", "text"),
        ]),
        ("⚔️ LINEAGE 2", [
            ("📊│estado-servidor", "text"),
            ("🏆│top-pvp", "text"),
            ("🏰│top-clanes", "text"),
            ("✨│heroes-olimpiadas", "text"),
            ("🗳️│votos", "text"),
        ]),
        ("💬 COMUNIDAD", [
            ("💬│general", "text"),
            ("🤝│busco-clan", "text"),
            ("🛒│comercio", "text"),
            ("🐛│reportar-bug", "text"),
            ("🎫│soporte", "text"),
        ]),
        ("🎙️ VOZ", [
            ("🔊 General", "voice"),
            ("⚔️ PvP Zone", "voice"),
            ("🏰 Siege", "voice"),
            ("🎲 AFK / Grind", "voice"),
        ]),
    ]

    created_categories = 0
    created_channels = 0
    skipped = 0

    existing_categories = {c.name: c for c in guild.categories}
    existing_channels = {c.name: c for c in guild.channels}

    for cat_name, channels in structure:
        if cat_name in existing_categories:
            category = existing_categories[cat_name]
            skipped += 1
        else:
            category = await guild.create_category(cat_name)
            created_categories += 1

        for ch_name, ch_type in channels:
            if ch_name in existing_channels:
                skipped += 1
                continue
            if ch_type == "text":
                await guild.create_text_channel(ch_name, category=category)
            else:
                await guild.create_voice_channel(ch_name, category=category)
            created_channels += 1

    embed = discord.Embed(
        title="✅ Setup completado",
        color=discord.Color.green(),
    )
    embed.add_field(name="Categorías creadas", value=str(created_categories), inline=True)
    embed.add_field(name="Canales creados", value=str(created_channels), inline=True)
    embed.add_field(name="Ya existían", value=str(skipped), inline=True)
    embed.set_footer(text="Lineage 2 H5 • Zona Zero")
    await interaction.followup.send(embed=embed, ephemeral=True)


# ── Emoji loader ─────────────────────────────────────────────────────────────

BASE_ICON_URL = "https://l2hub.info/s/icons-gf/"

L2_EMOJIS = [
    # (nombre_emoji, archivo)
    ("l2_duelist",          "duelist.png"),
    ("l2_dreadnought",      "dreadnought.png"),
    ("l2_phoenix_knight",   "phoenix_knight.png"),
    ("l2_hell_knight",      "hell_knight.png"),
    ("l2_adventurer",       "adventurer.png"),
    ("l2_sagittarius",      "sagittarius.png"),
    ("l2_archmage",         "archmage.png"),
    ("l2_soultaker",        "soultaker.png"),
    ("l2_arcana_lord",      "arcana_lord.png"),
    ("l2_cardinal",         "cardinal.png"),
    ("l2_hierophant",       "hierophant.png"),
    ("l2_evas_templar",     "evas_templar.png"),
    ("l2_sword_muse",       "sword_muse.png"),
    ("l2_wind_rider",       "wind_rider.png"),
    ("l2_moonlight_sentin", "moonlight_sentinel.png"),
    ("l2_mystic_muse",      "mystic_muse.png"),
    ("l2_elem_master",      "elemental_master.png"),
    ("l2_evas_saint",       "evas_saint.png"),
    ("l2_shillien_templ",   "shillien_templar.png"),
    ("l2_spectral_dancer",  "spectral_dancer.png"),
    ("l2_ghost_hunter",     "ghost_hunter.png"),
    ("l2_ghost_sentinel",   "ghost_sentinel.png"),
    ("l2_storm_screamer",   "storm_screamer.png"),
    ("l2_spectral_master",  "spectral_master.png"),
    ("l2_shillien_saint",   "shillien_saint.png"),
    ("l2_titan",            "titan.png"),
    ("l2_grand_khavatari",  "grand_khavatari.png"),
    ("l2_dominator",        "dominator.png"),
    ("l2_doomcryer",        "doomcryer.png"),
    ("l2_fortune_seeker",   "fortune_seeker.png"),
    ("l2_maestro",          "maestro.png"),
    ("l2_doombringer",      "doombringer.png"),
    ("l2_soul_hound",       "m_soul_hound.png"),
    ("l2_trickster",        "trickster.png"),
    ("l2_judicator",        "judicator.png"),
    ("l2_bladedancer",      "bladedancer.png"),
    ("l2_overlord",         "overlord.png"),
    ("l2_warcryer",         "warcryer.png"),
    ("l2_destroyer",        "destroyer.png"),
    ("l2_bounty_hunter",    "bounty_hunter.png"),
    ("l2_warsmith",         "warsmith.png"),
    ("l2_bishop",           "bishop.png"),
    ("l2_paladin",          "paladin.png"),
    ("l2_dark_avenger",     "dark_avenger.png"),
    ("l2_necromancer",      "necromancer.png"),
    ("l2_warlock",          "warlock.png"),
    ("l2_prophet",          "prophet.png"),
    ("l2_gladiator",        "gladiator.png"),
    ("l2_warlord",          "warlord.png"),
    ("l2_hawkeye",          "hawkeye.png"),
]


EMOJI_DIR = os.path.join(os.path.dirname(__file__), "emojis")


@bot.tree.command(name="emojis", description="Sube los emojis chibi de L2 al servidor (solo admins)")
@app_commands.checks.has_permissions(administrator=True)
async def cmd_emojis(interaction: discord.Interaction):
    await interaction.response.defer(ephemeral=True)
    guild = interaction.guild
    existing = {e.name for e in guild.emojis}
    added, skipped, failed = 0, 0, 0

    if not os.path.isdir(EMOJI_DIR):
        await interaction.followup.send(
            "No se encontro la carpeta `emojis/`. Ejecuta primero `python make_emojis.py`.",
            ephemeral=True,
        )
        return

    files = [f for f in os.listdir(EMOJI_DIR) if f.endswith(".png")]

    for filename in files:
        name = os.path.splitext(filename)[0][:32]
        if name in existing:
            skipped += 1
            continue
        if len(guild.emojis) >= guild.emoji_limit:
            await interaction.followup.send(
                f"Limite de emojis alcanzado ({guild.emoji_limit}). "
                f"Subidos: {added}, omitidos: {skipped}.",
                ephemeral=True,
            )
            return
        try:
            with open(os.path.join(EMOJI_DIR, filename), "rb") as f:
                img_bytes = f.read()
            await guild.create_custom_emoji(name=name, image=img_bytes)
            added += 1
            await asyncio.sleep(5)
        except (discord.Forbidden, discord.HTTPException):
            failed += 1
        except Exception:
            failed += 1

    embed = discord.Embed(title="Emojis chibi L2 subidos", color=discord.Color.gold())
    embed.add_field(name="Agregados", value=str(added), inline=True)
    embed.add_field(name="Ya existian", value=str(skipped), inline=True)
    embed.add_field(name="Fallidos", value=str(failed), inline=True)
    embed.description = "Usa los emojis escribiendo `:nombre_emoji:` en cualquier canal."
    await interaction.followup.send(embed=embed, ephemeral=True)


STICKER_DIR = os.path.join(os.path.dirname(__file__), "stickers")


@bot.tree.command(name="stickers", description="Sube los stickers chibi de L2 al servidor (solo admins)")
@app_commands.checks.has_permissions(administrator=True)
async def cmd_stickers(interaction: discord.Interaction):
    await interaction.response.defer(ephemeral=True)
    guild = interaction.guild

    if not os.path.isdir(STICKER_DIR):
        await interaction.followup.send(
            "No se encontro la carpeta `stickers/`. Ejecuta primero `python crop_stickers.py`.",
            ephemeral=True,
        )
        return

    existing = {s.name for s in guild.stickers}
    added, skipped, failed = 0, 0, 0
    files = [f for f in os.listdir(STICKER_DIR) if f.endswith(".png")]

    for filename in files:
        name = os.path.splitext(filename)[0][:30]
        if name in existing:
            skipped += 1
            continue
        if len(guild.stickers) >= guild.sticker_limit:
            await interaction.followup.send(
                f"Limite de stickers alcanzado ({guild.sticker_limit}). "
                f"Subidos: {added}, omitidos: {skipped}.",
                ephemeral=True,
            )
            return
        try:
            sticker_file = discord.File(
                os.path.join(STICKER_DIR, filename), filename=filename
            )
            await guild.create_sticker(
                name=name,
                description=f"Lineage 2 - {name.replace('_', ' ').title()}",
                emoji="⚔️",
                file=sticker_file,
            )
            added += 1
            await asyncio.sleep(5)
        except (discord.Forbidden, discord.HTTPException):
            failed += 1
        except Exception:
            failed += 1

    embed = discord.Embed(title="Stickers chibi L2 subidos", color=discord.Color.purple())
    embed.add_field(name="Agregados", value=str(added), inline=True)
    embed.add_field(name="Ya existian", value=str(skipped), inline=True)
    embed.add_field(name="Fallidos", value=str(failed), inline=True)
    embed.description = "Los stickers aparecen en el menu de stickers del chat."
    await interaction.followup.send(embed=embed, ephemeral=True)


# ── Reglas de la comunidad ────────────────────────────────────────────────────

@bot.tree.command(name="reglas", description="Publica las reglas de la comunidad (solo admins)")
@app_commands.checks.has_permissions(administrator=True)
async def cmd_reglas(interaction: discord.Interaction):
    """Envía un embed con las reglas de la comunidad al canal actual."""
    await interaction.response.defer(ephemeral=True)

    # ── Embed principal ──
    embed = discord.Embed(
        title="📜  REGLAS DE LA COMUNIDAD  📜",
        description=(
            "Bienvenido a **L2 Zona Zero**. Para mantener un ambiente sano y "
            "competitivo, todos los jugadores deben respetar las siguientes reglas. "
            "El incumplimiento puede resultar en **mute, ban temporal o ban permanente** "
            "según la gravedad de la falta."
        ),
        color=0x7c3aed,
    )
    embed.set_thumbnail(url="https://lineage2zonazero.com/assets/img/beta-bg.jpg")

    # ── Sección 1: Respeto ──
    embed.add_field(
        name="⚔️  1. RESPETO ENTRE JUGADORES",
        value=(
            "• No se toleran **insultos, acoso, amenazas** ni humillaciones hacia otros jugadores.\n"
            "• Está prohibido el **bullying, hostigamiento** o persecución sistemática.\n"
            "• Resolvé tus conflictos de forma madura. El PvP es en el juego, no en el chat."
        ),
        inline=False,
    )

    # ── Sección 2: Discriminación ──
    embed.add_field(
        name="🚫  2. CERO DISCRIMINACIÓN",
        value=(
            "• Queda **estrictamente prohibido** todo tipo de **racismo, xenofobia, "
            "homofobia, sexismo** o cualquier forma de discriminación.\n"
            "• No se permiten **símbolos, nombres de personajes o clanes** con contenido "
            "ofensivo, discriminatorio o de odio.\n"
            "• Esto aplica al chat del juego, Discord, y cualquier medio de la comunidad."
        ),
        inline=False,
    )

    # ── Sección 3: Chat ──
    embed.add_field(
        name="💬  3. USO DEL CHAT",
        value=(
            "• No hacer **spam, flood** ni publicidad de otros servidores.\n"
            "• No compartir **contenido NSFW, gore** o material inapropiado.\n"
            "• El chat de **comercio** es solo para compra/venta. No abusar.\n"
            "• Usar los canales de Discord correspondientes para cada tema."
        ),
        inline=False,
    )

    # ── Sección 4: Fair Play ──
    embed.add_field(
        name="🎮  4. JUEGO LIMPIO (FAIR PLAY)",
        value=(
            "• Queda prohibido el uso de **bots, hacks, exploits** o cualquier "
            "programa externo que modifique el cliente del juego.\n"
            "• No se permite el **abuso de bugs**. Si encontrás uno, reportalo al Staff.\n"
            "• Está prohibido el **RMT** (Real Money Trading) — compra/venta de items "
            "o cuentas por dinero real fuera de la web oficial.\n"
            "• No se permite el **feed de PvP/PK** (matar cuentas propias para subir stats)."
        ),
        inline=False,
    )

    # ── Sección 5: Cuentas ──
    embed.add_field(
        name="🔑  5. CUENTAS Y SEGURIDAD",
        value=(
            "• Cada jugador es **responsable de su cuenta**. No compartir credenciales.\n"
            "• Está prohibido **hackear, robar o acceder** a cuentas ajenas.\n"
            "• El Staff **nunca** pedirá tu contraseña. No caigas en estafas.\n"
            "• Las cuentas son **personales e intransferibles**."
        ),
        inline=False,
    )

    # ── Sección 6: Staff ──
    embed.add_field(
        name="👑  6. RESPETO AL STAFF",
        value=(
            "• Tratar al Staff con **respeto**. Son voluntarios que ayudan a la comunidad.\n"
            "• No insistir ni acosar al Staff por items, beneficios o unbans.\n"
            "• Las decisiones del Staff son **finales**. Podés apelar de forma respetuosa.\n"
            "• Hacerse pasar por Staff es motivo de **ban permanente**."
        ),
        inline=False,
    )

    # ── Sección 7: Sanciones ──
    embed.add_field(
        name="⚖️  7. SANCIONES",
        value=(
            "```\n"
            "🟡 Falta leve    → Advertencia / Mute 1-24hs\n"
            "🟠 Falta media   → Mute 1-7 días / Ban temporal\n"
            "🔴 Falta grave   → Ban permanente sin apelación\n"
            "```\n"
            "*El Staff se reserva el derecho de aplicar la sanción que considere "
            "apropiada según el contexto.*"
        ),
        inline=False,
    )

    embed.set_footer(
        text="L2 Zona Zero • Las reglas pueden actualizarse en cualquier momento • lineage2zonazero.com"
    )
    embed.timestamp = datetime.now(timezone.utc)

    # Enviar al canal (visible para todos)
    await interaction.channel.send(embed=embed)
    await interaction.followup.send("✅ Reglas publicadas en el canal.", ephemeral=True)


# ══════════════════════════════════════════════════════════════════════════════
# SISTEMA DE NIVELES
# ══════════════════════════════════════════════════════════════════════════════

WELCOME_CHANNEL = "la-zeroneta"

# Niveles: (xp_requerido, nombre, emoji)
LEVELS = [
    (0,      "Novato",              "🌱"),
    (100,    "Aprendiz",            "📘"),
    (300,    "Guerrero",            "⚔️"),
    (600,    "Veterano",            "🛡️"),
    (1000,   "Elite",               "💎"),
    (1800,   "Comandante",          "🎖️"),
    (3000,   "Heroe",               "🏆"),
    (5000,   "Legendario",          "👑"),
    (8000,   "Mitico",              "🔱"),
    (12000,  "Inmortal",            "⭐"),
]

# Cooldown para XP por mensaje (evita spam): {user_id: last_xp_time}
_xp_cooldowns = {}
XP_COOLDOWN_SECS = 30
XP_PER_MESSAGE = (15, 25)  # rango aleatorio


def get_level_info(xp: int):
    """Retorna (nivel_idx, nombre, emoji, xp_actual_en_nivel, xp_para_siguiente)."""
    for i in range(len(LEVELS) - 1, -1, -1):
        if xp >= LEVELS[i][0]:
            next_xp = LEVELS[i + 1][0] if i + 1 < len(LEVELS) else None
            progress_in = xp - LEVELS[i][0]
            needed = (next_xp - LEVELS[i][0]) if next_xp else 0
            return i, LEVELS[i][1], LEVELS[i][2], progress_in, needed
    return 0, LEVELS[0][1], LEVELS[0][2], 0, LEVELS[1][0]


def make_progress_bar(current, total, length=16):
    if total <= 0:
        return "▓" * length + " MAX"
    filled = int(length * current / total)
    filled = min(filled, length)
    return "▓" * filled + "░" * (length - filled)


async def ensure_levels_table():
    """Crea la tabla de niveles si no existe."""
    await execute("""
        CREATE TABLE IF NOT EXISTS discord_levels (
            user_id BIGINT PRIMARY KEY,
            username VARCHAR(100) NOT NULL DEFAULT '',
            xp INT NOT NULL DEFAULT 0,
            level INT NOT NULL DEFAULT 0,
            messages INT NOT NULL DEFAULT 0,
            last_xp DATETIME DEFAULT NULL
        )
    """)


async def get_user_xp(user_id: int) -> dict:
    rows = await query("SELECT * FROM discord_levels WHERE user_id = %s", (user_id,))
    return rows[0] if rows else None


async def add_xp(user_id: int, username: str, amount: int) -> tuple:
    """Agrega XP. Retorna (new_xp, old_level, new_level)."""
    row = await get_user_xp(user_id)
    if row is None:
        await execute(
            "INSERT INTO discord_levels (user_id, username, xp, level, messages, last_xp) "
            "VALUES (%s, %s, %s, 0, 1, NOW())",
            (user_id, username, amount)
        )
        old_lvl = 0
        new_xp = amount
    else:
        old_lvl = row["level"]
        new_xp = row["xp"] + amount
        await execute(
            "UPDATE discord_levels SET xp = %s, username = %s, messages = messages + 1, "
            "last_xp = NOW() WHERE user_id = %s",
            (new_xp, username, user_id)
        )

    new_lvl_idx = get_level_info(new_xp)[0]
    if new_lvl_idx != old_lvl:
        await execute(
            "UPDATE discord_levels SET level = %s WHERE user_id = %s",
            (new_lvl_idx, user_id)
        )

    return new_xp, old_lvl, new_lvl_idx


# ── Comando /nivel ──

@bot.tree.command(name="nivel", description="Muestra tu nivel y XP en la comunidad")
@app_commands.describe(usuario="Usuario a consultar (opcional)")
async def cmd_nivel(interaction: discord.Interaction, usuario: discord.Member = None):
    target = usuario or interaction.user
    row = await get_user_xp(target.id)

    if not row:
        await interaction.response.send_message(
            f"{target.display_name} aun no tiene actividad registrada.", ephemeral=True
        )
        return

    lvl_idx, lvl_name, emoji, progress, needed = get_level_info(row["xp"])
    bar = make_progress_bar(progress, needed)

    embed = discord.Embed(
        title=f"{emoji}  {target.display_name}",
        color=0x7c3aed,
    )
    embed.set_thumbnail(url=target.display_avatar.url)
    embed.add_field(name="Nivel", value=f"**{lvl_idx}** — {emoji} {lvl_name}", inline=True)
    embed.add_field(name="XP Total", value=f"**{row['xp']:,}**", inline=True)
    embed.add_field(name="Mensajes", value=f"**{row['messages']:,}**", inline=True)

    if needed > 0:
        next_emoji = LEVELS[lvl_idx + 1][2] if lvl_idx + 1 < len(LEVELS) else ""
        next_name = LEVELS[lvl_idx + 1][1] if lvl_idx + 1 < len(LEVELS) else "MAX"
        embed.add_field(
            name=f"Progreso hacia {next_emoji} {next_name}",
            value=f"`{bar}` {progress}/{needed} XP",
            inline=False,
        )
    else:
        embed.add_field(name="Rango", value="⭐ **NIVEL MAXIMO ALCANZADO** ⭐", inline=False)

    embed.set_footer(text="Gana XP participando en la comunidad")
    await interaction.response.send_message(embed=embed)


# ── Comando /ranking (niveles) ──

@bot.tree.command(name="topnivel", description="Top 10 usuarios por nivel en la comunidad")
async def cmd_topnivel(interaction: discord.Interaction):
    rows = await query(
        "SELECT username, xp, level, messages FROM discord_levels ORDER BY xp DESC LIMIT 10"
    )
    if not rows:
        await interaction.response.send_message("Aun no hay datos de niveles.", ephemeral=True)
        return

    embed = discord.Embed(
        title="🏆  TOP 10 — Niveles de la Comunidad",
        color=0xf59e0b,
    )

    medals = ["🥇", "🥈", "🥉"]
    desc_lines = []
    for i, r in enumerate(rows):
        lvl_idx, lvl_name, emoji, _, _ = get_level_info(r["xp"])
        medal = medals[i] if i < 3 else f"`{i+1}.`"
        desc_lines.append(
            f"{medal} **{r['username']}** — {emoji} {lvl_name} (Lvl {lvl_idx}) — "
            f"`{r['xp']:,} XP` · {r['messages']:,} msgs"
        )

    embed.description = "\n".join(desc_lines)
    embed.set_footer(text="Participa en la comunidad para subir de nivel")
    await interaction.response.send_message(embed=embed)


# ── Evento: XP por mensaje ──

@bot.event
async def on_message(message: discord.Message):
    if message.author.bot:
        return

    # Procesar comandos prefix primero
    await bot.process_commands(message)

    # Cooldown check
    now = datetime.now(timezone.utc).timestamp()
    last = _xp_cooldowns.get(message.author.id, 0)
    if now - last < XP_COOLDOWN_SECS:
        return

    _xp_cooldowns[message.author.id] = now

    # Dar XP aleatorio
    xp_amount = random.randint(*XP_PER_MESSAGE)
    try:
        new_xp, old_lvl, new_lvl = await add_xp(
            message.author.id, message.author.display_name, xp_amount
        )

        # Si subio de nivel, anunciar
        if new_lvl > old_lvl:
            lvl_idx, lvl_name, emoji, _, _ = get_level_info(new_xp)
            embed = discord.Embed(
                title=f"🎉  SUBIO DE NIVEL",
                description=(
                    f"**{message.author.mention}** alcanzo el nivel **{lvl_idx}**!\n\n"
                    f"Rango: {emoji} **{lvl_name}**"
                ),
                color=0x53FC18,
            )
            embed.set_thumbnail(url=message.author.display_avatar.url)
            await message.channel.send(embed=embed)
    except Exception as e:
        print(f"[Levels] Error procesando XP: {e}")


# ── Evento: Bienvenida a nuevos usuarios ──

@bot.event
async def on_member_join(member: discord.Member):
    # Buscar canal la-zeroneta
    channel = discord.utils.get(member.guild.text_channels, name=WELCOME_CHANNEL)
    if not channel:
        print(f"[Welcome] Canal '{WELCOME_CHANNEL}' no encontrado")
        return

    member_count = member.guild.member_count

    embed = discord.Embed(
        title="⚔️  NUEVO GUERRERO EN LA ZONA  ⚔️",
        description=(
            f"Bienvenido {member.mention} a **L2 Zona Zero**!\n\n"
            f"Sos el miembro **#{member_count}** de la comunidad.\n\n"
            "🗡️ Leé las reglas en el canal de reglas\n"
            "🎮 Descarga el cliente desde la web\n"
            "💬 Participa en la comunidad para subir de nivel\n\n"
            "**Tu aventura comienza ahora!**"
        ),
        color=0x00d4ff,
    )
    embed.set_thumbnail(url=member.display_avatar.url)
    embed.set_footer(text="lineage2zonazero.com")
    embed.timestamp = datetime.now(timezone.utc)

    await channel.send(embed=embed)

    # Registrar en tabla de niveles
    try:
        row = await get_user_xp(member.id)
        if not row:
            await execute(
                "INSERT INTO discord_levels (user_id, username, xp, level, messages) "
                "VALUES (%s, %s, 0, 0, 0)",
                (member.id, member.display_name)
            )
    except Exception as e:
        print(f"[Welcome] Error registrando usuario: {e}")


# ── Events ────────────────────────────────────────────────────────────────────

@bot.event
async def on_ready():
    guild = discord.Object(id=GUILD_ID) if GUILD_ID else None
    if guild:
        bot.tree.copy_global_to(guild=guild)
        await bot.tree.sync(guild=guild)
    else:
        await bot.tree.sync()
    # Crear tabla de niveles
    try:
        await ensure_levels_table()
        print("Tabla discord_levels verificada/creada")
    except Exception as e:
        print(f"Error creando tabla discord_levels: {e}")
    print(f"Bot conectado como {bot.user} | Comandos sincronizados")
    status_task.start()


@tasks.loop(minutes=5)
async def status_task():
    online = await check_server_online()
    if online:
        rows = await query("SELECT COUNT(*) AS cnt FROM characters WHERE online = 1")
        players = rows[0]["cnt"] if rows else 0
        activity = discord.Activity(
            type=discord.ActivityType.playing,
            name=f"L2 H5 • {players} jugadores online",
        )
        status = discord.Status.online
    else:
        activity = discord.Activity(
            type=discord.ActivityType.watching, name="Servidor offline"
        )
        status = discord.Status.do_not_disturb
    await bot.change_presence(status=status, activity=activity)


if __name__ == "__main__":
    if not TOKEN:
        raise ValueError("Falta DISCORD_TOKEN en el archivo .env")
    bot.run(TOKEN)
