package net.easecation.clientsettings.feature.obsoverlay;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Creates unlinkable, connection-scoped aliases without retaining player names. */
public final class PlayerAliasService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TRANSLATION_PREFIX = "format.ecclientsettings.obs_overlay.player_alias.";
    private static final String NAME_PREFIX = "name.ecclientsettings.obs_overlay.player_alias.";
    private static final String SAFE_BASE32 = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int SECRET_BYTES = 32;
    private static final int CODE_LENGTH = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String[] ADJECTIVES = {
            "amber", "azure", "brave", "bright",
            "calm", "clever", "coral", "crystal",
            "gentle", "golden", "jade", "lively",
            "quiet", "silver", "swift", "warm"
    };
    private static final String[] ANIMALS = {
            "badger", "bear", "cat", "crane",
            "deer", "dolphin", "falcon", "fox",
            "hare", "lynx", "otter", "owl",
            "panda", "rabbit", "tiger", "wolf"
    };

    // Bright, fixed colors remain readable against Minecraft's dark name-tag background.
    private static final int[] DISTINCT_PALETTE = {
            0xF2C94C, 0xF2994A, 0xEB5757, 0xFF6B9D,
            0xD98CFF, 0xA78BFA, 0x56CCF2, 0x2D9CDB,
            0x22D3EE, 0x2DCE89, 0x6FCF97, 0xA3E635,
            0xE5E7EB, 0xFDE68A, 0xFDBA74, 0xFCA5A5
    };

    private static final Map<UUID, AliasIdentity> ALIASES_BY_PLAYER = new HashMap<>();
    private static final Map<String, UUID> PLAYERS_BY_CODE = new HashMap<>();
    private static byte[] secret = newSecret();

    private PlayerAliasService() {
    }

    public static synchronized Component aliasFor(
            Player player,
            PlayerAliasFormat format,
            PlayerAliasColorMode colorMode
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(colorMode, "colorMode");

        AliasIdentity identity = identityFor(player.getUUID());
        MutableComponent alias = switch (format) {
            case FRIENDLY -> Component.translatable(
                    TRANSLATION_PREFIX + "friendly",
                    Component.translatable(NAME_PREFIX + "adjective." + ADJECTIVES[identity.adjectiveIndex()]),
                    Component.translatable(NAME_PREFIX + "animal." + ANIMALS[identity.animalIndex()]),
                    identity.code()
            );
            case NUMBERED -> Component.translatable(TRANSLATION_PREFIX + "numbered", identity.code());
        };
        return applyColor(alias, player, colorMode, identity.paletteIndex());
    }

    public static synchronized void reset() {
        Arrays.fill(secret, (byte) 0);
        secret = newSecret();
        ALIASES_BY_PLAYER.clear();
        PLAYERS_BY_CODE.clear();
    }

    private static AliasIdentity identityFor(UUID playerId) {
        AliasIdentity cached = ALIASES_BY_PLAYER.get(playerId);
        if (cached != null && playerId.equals(PLAYERS_BY_CODE.get(cached.code()))) {
            return cached;
        }

        for (int attempt = 0; attempt < Integer.MAX_VALUE; attempt++) {
            byte[] digest = digest(playerId, attempt);
            String code = base32Code(digest);
            UUID currentOwner = PLAYERS_BY_CODE.get(code);
            if (currentOwner != null && !currentOwner.equals(playerId)) {
                continue;
            }

            AliasIdentity identity = new AliasIdentity(
                    code,
                    Byte.toUnsignedInt(digest[4]) & 0x0F,
                    Byte.toUnsignedInt(digest[5]) & 0x0F,
                    Byte.toUnsignedInt(digest[6]) & 0x0F
            );
            ALIASES_BY_PLAYER.put(playerId, identity);
            PLAYERS_BY_CODE.put(code, playerId);
            return identity;
        }
        throw new IllegalStateException("Could not allocate a unique player alias");
    }

    private static byte[] digest(UUID playerId, int attempt) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            ByteBuffer input = ByteBuffer.allocate(attempt == 0 ? 16 : 20)
                    .putLong(playerId.getMostSignificantBits())
                    .putLong(playerId.getLeastSignificantBits());
            if (attempt != 0) {
                input.putInt(attempt);
            }
            return mac.doFinal(input.array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is unavailable", exception);
        }
    }

    private static String base32Code(byte[] digest) {
        int bits = ByteBuffer.wrap(digest).getInt() & 0x01FF_FFFF;
        char[] code = new char[CODE_LENGTH];
        for (int index = CODE_LENGTH - 1; index >= 0; index--) {
            code[index] = SAFE_BASE32.charAt(bits & 0x1F);
            bits >>>= 5;
        }
        return new String(code);
    }

    private static Component applyColor(
            MutableComponent alias,
            Player player,
            PlayerAliasColorMode colorMode,
            int paletteIndex
    ) {
        int rgb = switch (colorMode) {
            case DISTINCT -> DISTINCT_PALETTE[paletteIndex];
            case TEAM -> teamColor(player);
            case WHITE -> 0xFFFFFF;
        };
        return alias.withColor(rgb);
    }

    private static int teamColor(Player player) {
        PlayerTeam team = player.getTeam();
        ChatFormatting formatting = team == null ? null : team.getColor();
        Integer rgb = formatting == null ? null : formatting.getColor();
        return rgb == null ? 0xFFFFFF : rgb;
    }

    private static byte[] newSecret() {
        byte[] next = new byte[SECRET_BYTES];
        RANDOM.nextBytes(next);
        return next;
    }

    private record AliasIdentity(String code, int adjectiveIndex, int animalIndex, int paletteIndex) {
    }
}
