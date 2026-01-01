package me.reimnop.d4f.customevents.constraints;

import eu.pb4.placeholders.api.PlaceholderHandler;
import me.reimnop.d4f.mixin.ServerPlayerEntityAccessor;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class OperatorConstraintProcessor implements ConstraintProcessor {

    private final boolean isOp;

    public OperatorConstraintProcessor(ServerPlayerEntity playerEntity) {
        // In 1.21.11, check if player has permission level >= 2 (operator level)
        var server = ((ServerPlayerEntityAccessor) playerEntity).getServer();
        // Simple heuristic: check if player is listed in the ops list by checking the entries
        boolean foundInOps = false;
        if (server != null) {
            for (var entry : server.getPlayerManager().getOpList().values()) {
                if (entry.getKey().equals(playerEntity.getGameProfile())) {
                    foundInOps = true;
                    break;
                }
            }
        }
        isOp = foundInOps;
    }

    @Override
    public void loadArguments(List<String> arguments) {
    }

    @Override
    public boolean satisfied() {
        return isOp;
    }

    @Nullable
    @Override
    public Map<Identifier, PlaceholderHandler> getHandlers() {
        return null;
    }
}
