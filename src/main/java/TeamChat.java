import static org.spongepowered.api.text.TextTemplate.arg;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.channel.MutableMessageChannel;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.transform.SimpleTextFormatter;
import org.spongepowered.api.text.transform.SimpleTextTemplateApplier;

import java.util.Optional;

import javax.annotation.Nullable;

@Plugin(id = "teamchat")
public final class TeamChat {

    private static final TextTemplate TEMPLATE = TextTemplate.of(
            arg("name")
    );

    @Listener
    public void onChat(final MessageChannelEvent event, @First final Player player){
        final Scoreboard scoreboard = Sponge.getGame().getServer().getServerScoreboard().orElse(null);
        final MessageChannel         originalChan = event.getOriginalChannel();
        final MessageChannel        immutableChan = event.getChannel().orElse(originalChan);
        final MutableMessageChannel          chan = immutableChan.asMutable();
        chan.clearMembers();
        immutableChan
                .getMembers().stream()
                .filter(
                    rec -> notPlayer(rec) || isSameTeam(player, rec, scoreboard) || areBothSpectators(player , rec)
                )
                .forEach(
                    chan::addMember
                );
        final SimpleTextFormatter header = event.getFormatter().getHeader();
        final SimpleTextTemplateApplier stta = new SimpleTextTemplateApplier(TEMPLATE);
        stta.setParameter("name", teamColor(player));
        header.insert(0, stta);
        event.setChannel(chan);

    }

    private static Text teamColor(final Player p) {
        final Scoreboard scoreboard = Sponge.getGame().getServer().getServerScoreboard().orElseThrow(TeamChat::needsScoreboardError);
        final Text pTeamRepresentation = p.getTeamRepresentation();
        final Optional<Team> pTeamOpt = scoreboard.getMemberTeam(pTeamRepresentation);
        return pTeamOpt
            .map (Team::getDisplayName)
            .orElse (
                spectatorName("DEAD")
            );
    }

    private static Text spectatorName(final String name) {
        return Text.of(TextColors.GRAY, TextStyles.STRIKETHROUGH, name);
    }

    private static IllegalStateException needsScoreboardError(){
        return new IllegalStateException("TeamChat requires server to support scoreboards.");
    }

    private static boolean isSameTeam(final Player p, final MessageReceiver receiver, @Nullable final Scoreboard scoreboard){
        //consoles etc
        if(notPlayer(receiver)) return false;
        if(null == scoreboard) return false;
        final Player other = (Player) receiver;
        final Text teamRepresentation1 = p.getTeamRepresentation();
        final Optional<Team> myTeam = scoreboard.getMemberTeam(teamRepresentation1);
        if(!myTeam.isPresent()) {
            return true;
        }
        final Text teamRepresentation = other.getTeamRepresentation();
        return myTeam.get().getMembers().contains(teamRepresentation);
    }

    private static boolean areBothSpectators(final Player p, final MessageReceiver receiver) {
        if(notPlayer(receiver)) return false;
        final boolean pSpectate = isSpectating(p);
        final boolean oSpectate = isSpectating((Player) receiver);
        return pSpectate && oSpectate;
    }

    private static boolean isSpectating(final Player player){
        return player.get(Keys.GAME_MODE)
                     .map(TeamChat::isSpectator)
                     .orElse(false);
    }

    private static boolean isSpectator(final GameMode gm){
        return GameModes.SPECTATOR.equals(gm);
    }

    private static boolean notPlayer(final MessageReceiver rec) {
        return ! (rec instanceof Player);
    }

}
