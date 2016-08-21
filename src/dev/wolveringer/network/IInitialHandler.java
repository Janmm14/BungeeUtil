package dev.wolveringer.network;

import io.netty.buffer.ByteBuf;

import java.lang.reflect.Field;

import dev.wolveringer.chat.ChatColor.ChatColorUtils;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.packet.Kick;
import net.md_5.bungee.protocol.packet.LoginSuccess;
import dev.wolveringer.BungeeUtil.AsyncCatcher;
import dev.wolveringer.BungeeUtil.ClientVersion;
import dev.wolveringer.BungeeUtil.Player;
import dev.wolveringer.BungeeUtil.configuration.Configuration;
import dev.wolveringer.BungeeUtil.packets.Packet;
import dev.wolveringer.BungeeUtil.packets.PacketPlayOutEntityEffect;
import dev.wolveringer.BungeeUtil.packets.PacketPlayOutGameStateChange;
import dev.wolveringer.BungeeUtil.packets.PacketPlayOutPlayerListHeaderFooter;
import dev.wolveringer.BungeeUtil.packets.PacketPlayOutPosition;
import dev.wolveringer.BungeeUtil.packets.PacketPlayOutRemoveEntityEffect;
import dev.wolveringer.BungeeUtil.packets.PacketPlayOutUpdateHealth;
import dev.wolveringer.chat.ChatSerializer;
import dev.wolveringer.chat.IChatBaseComponent;
import dev.wolveringer.network.channel.ChannelWrapper;

public abstract class IInitialHandler extends InitialHandler {
	public final static Field CHANNEL_FIELD;

	static {
		Field f;
		try {
			f = InitialHandler.class.getDeclaredField("ch");
			f.setAccessible(true);
		}
		catch (NoSuchFieldException | SecurityException e) {
			f = null;
			e.printStackTrace();
		}
		CHANNEL_FIELD = f;
	}

	public IInitialHandler(ProxyServer instance, ListenerInfo listenerInfo, Decoder a, Encoder b) {
		super(BungeeCord.getInstance(), listenerInfo);
		this.a = a;
		this.b = b;
		if (a != null) a.setInitHandler(this);
		if (b != null) b.setInitHandler(this);
	}

	public boolean isConnected = false;
	private boolean isDisconnecting = false;
	private Encoder b;
	private Decoder a;
	private short transaktionId;
	private short window;
	private IChatBaseComponent[] tab = new IChatBaseComponent[2];


	public Encoder getEncoder() {
		return b;
	}

	public Decoder getDecoder() {
		return a;
	}

	@Override
	public void connected(final net.md_5.bungee.netty.ChannelWrapper channel) throws Exception {
		super.connected(new ChannelWrapper(channel, this));
	}

	@Override
	public void handle(LoginSuccess loginSuccess) throws Exception {
		super.handle(loginSuccess);
	}

	public abstract Player getPlayer();

	@Override
	public void disconnect(String reason) {
		disconnect(TextComponent.fromLegacyText(reason));
	}

	@Override
	public void disconnect(final BaseComponent... reason) {
		if (isDisconnecting) return;
		isDisconnecting = true;
		if (getHandshake() != null && getHandshake().getRequestedProtocol() == 2) {
			if(getEncoder().getProtocoll() == Protocol.HANDSHAKE)
				setProtocol(Protocol.LOGIN);
			unsafe().sendPacket(new Kick(ComponentSerializer.toString(reason)));
		}
		closeChannel();
	}

	public void disconnect(Exception e) {
		disconnect(e, 10);
	}

	public void disconnect(Exception e, int stackDeep) {
		if (isDisconnecting) return;
		if (getChannel().isClosed()) { return; }
		String message = "" + ChatColorUtils.COLOR_CHAR + "4Error Message: " + ChatColorUtils.COLOR_CHAR + "b" + e.getLocalizedMessage() + "\n";
		for (int i = 0; i < (e.getStackTrace().length > stackDeep ? stackDeep : e.getStackTrace().length); i++) {
			StackTraceElement ex = e.getStackTrace()[i];
			if(ex.getMethodName().equalsIgnoreCase("channelRead") && ex.getClassName().equalsIgnoreCase("io.netty.handler.codec.MessageToMessageDecoder") && ex.getLineNumber() == 89)
				break;
			message += format(ex) + "\n";
		}
		disconnect(message);
	}

	public void closeChannel() {
		if (!getChannel().isClosed()) getChannel().close();
		if (isConnected) {
			if (getPlayer().getInventoryView() != null) getPlayer().getInventoryView().unsave().getModificableViewerList().remove(this);
			getPlayer().getPlayerInventory().reset();
			isConnected = false;
			b = null;
			a = null;
			tab = null;
		}
	}

	public void sendPacket(Packet p) {
		AsyncCatcher.catchOp("Packet cant be sending async!");
		ByteBuf b = p.getByteBuf(ClientVersion.fromProtocoll(getVersion()));
		getChannel().getHandle().writeAndFlush(b);
		p = null;
	}

	public void sendPacketToServer(Packet p) {
		AsyncCatcher.catchOp("Packet cant be sending async!");
		ByteBuf b = p.getByteBuf(ClientVersion.fromProtocoll(this.getVersion()));
		((ServerConnection) getPlayer().getServer()).getCh().write(b);
	}

	public void setProtocol(Protocol p) {
		a.setProtocol(p);
		b.setProtocol(p);
	}

	private String format(StackTraceElement e) {
		return "" + ChatColorUtils.COLOR_CHAR + "eat " + ChatColorUtils.COLOR_CHAR + "5" + e.getClassName() + "." + ChatColorUtils.COLOR_CHAR + "b" + e.getMethodName() + (e.getFileName() != null ? "(" + ChatColorUtils.COLOR_CHAR + "a" + e.getFileName() + ":"+e.getLineNumber() + ChatColorUtils.COLOR_CHAR + "b)" : (e.getFileName() != null) && (e.getLineNumber() >= 0) ? "(" + ChatColorUtils.COLOR_CHAR + "a" + e.getFileName() + "" + ChatColorUtils.COLOR_CHAR + "b:" + ChatColorUtils.COLOR_CHAR + "c" + e.getLineNumber() + "" + ChatColorUtils.COLOR_CHAR + "b)" : e.isNativeMethod() ? "" + ChatColorUtils.COLOR_CHAR + "1(Native Method)" : "" + ChatColorUtils.COLOR_CHAR + "c(Unknown Source)");
	}

	@Deprecated
	public void resetClient() {
		for (int i = 1; i < 24; i++)
			sendPacket(new PacketPlayOutRemoveEntityEffect(getEntityId(), i));
		sendPacket(new PacketPlayOutPosition(getPlayer().getLocation().add(0, 10000, 0), true));
		sendPacket(new PacketPlayOutEntityEffect(getEntityId(), 15, 1, 100000, true));
		sendPacket(new PacketPlayOutGameStateChange(3, 0));
		sendPacket(new PacketPlayOutUpdateHealth(20F, 20, 0F));
		resetInventory();
	}

	@Deprecated
	public void resetInventory() {
		for (int i = 0; i < getPlayer().getPlayerInventory().getContains().length; i++)
			getPlayer().getPlayerInventory().setItem(i, null);
		getPlayer().updateInventory();
	}

	public int getEntityId() {
		try {
			Field f = UserConnection.class.getDeclaredField("clientEntityId");
			f.setAccessible(true);
			if (BungeeCord.getInstance().getPlayer(getPlayer().getName()) == null) return -1;
			return f.getInt(BungeeCord.getInstance().getPlayer(getPlayer().getName()));
		}
		catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	public ChannelWrapper getChannel() {
		try {
			return (ChannelWrapper) CHANNEL_FIELD.get(this);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	public short getTransaktionId() {
		return transaktionId;
	}

	public void setTransaktionId(short transaktionId) {
		this.transaktionId = transaktionId;
	}

	public void setWindow(short window) {
		this.window = window;
	}

	public short getWindow() {
		return window;
	}

	public IChatBaseComponent[] getTabHeader() {
		return tab;
	}

	public void setTabHeader(IChatBaseComponent header, IChatBaseComponent footer) {
		this.tab = new IChatBaseComponent[] { header, footer };
		sendPacket(new PacketPlayOutPlayerListHeaderFooter(header, footer));
	}

	public void setTabHeaderFromPacket(IChatBaseComponent header, IChatBaseComponent footer) {
		this.tab = new IChatBaseComponent[] { header, footer };
	}

	@Override
	public String toString() {
		if (Configuration.isTerminalColored()) return "[" + (getHandshake().getRequestedProtocol() == 2 ? "" + ChatColorUtils.COLOR_CHAR + "aGAME" : "" + ChatColorUtils.COLOR_CHAR + "ePING") + "" + ChatColorUtils.COLOR_CHAR + "7][" + (getHandshake().getRequestedProtocol() == 0 ? "" + ChatColorUtils.COLOR_CHAR + "6" + getName() : "" + ChatColorUtils.COLOR_CHAR + "c" + getAddress().getHostString()) + "" + ChatColorUtils.COLOR_CHAR + "7]";
		else return super.toString();
	}
}
